package com.ssairen.backend.domain.callsession.service;

import com.ssairen.backend.domain.callsession.dto.CallSessionResponse;
import com.ssairen.backend.domain.callsession.dto.CreateCallSessionRequest;
import com.ssairen.backend.domain.callsession.dto.SessionCompletionResult;
import com.ssairen.backend.domain.callsession.dto.TranscriptAcceptResult;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.TranscriptChunk;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.callsession.repository.TranscriptChunkRepository;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallSessionService {

    /*
     * 이 서비스는 Flutter와의 실시간 통신 관점에서 "통화 세션"을 관리한다.
     * 다만 실제 비즈니스 데이터는 ERD 기준으로 users / cases 테이블에 저장하고,
     * call_sessions / transcript_chunks는 WebSocket 제어와 순서 보장을 위한 기술 테이블로 사용한다.
     */
    private final CallSessionRepository callSessionRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final UserRepository userRepository;
    private final FraudCaseRepository fraudCaseRepository;

    /*
     * 현재 구현에서는 실제 AI 분석 큐를 붙이지 않았으므로
     * "분석을 돌릴 만큼 텍스트가 충분히 누적됐는지"를 문자 수 기준으로만 판단한다.
     */
    private final long analysisThresholdCharacters;

    public CallSessionService(
            CallSessionRepository callSessionRepository,
            TranscriptChunkRepository transcriptChunkRepository,
            UserRepository userRepository,
            FraudCaseRepository fraudCaseRepository,
            @Value("${ssairen.transcript.analysis-threshold-characters:1000}") long analysisThresholdCharacters
    ) {
        this.callSessionRepository = callSessionRepository;
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.userRepository = userRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.analysisThresholdCharacters = analysisThresholdCharacters;
    }

    @Transactional
    public CallSessionResponse createSession(CreateCallSessionRequest request) {
        return callSessionRepository.findByExternalCallId(request.externalCallId())
                .map(CallSessionResponse::from)
                .orElseGet(() -> {
                    /*
                     * 세션 생성 시점에 피해자 정보는 users 테이블에,
                     * 탐지 진행 단위는 cases 테이블에 먼저 만든다.
                     * 그 뒤 둘을 참조하는 기술 세션을 call_sessions에 저장한다.
                     */
                    User victim = resolveVictim(request);
                    FraudCase fraudCase = fraudCaseRepository.save(new FraudCase(victim, request.startedAt()));
                    CallSession session = new CallSession(
                            UUID.randomUUID().toString(),
                            request.externalCallId(),
                            request.deviceId(),
                            victim,
                            fraudCase,
                            request.startedAt()
                    );
                    return CallSessionResponse.from(callSessionRepository.save(session));
                });
    }

    @Transactional(readOnly = true)
    public CallSessionResponse getSession(String sessionId) {
        return CallSessionResponse.from(findSession(sessionId));
    }

    @Transactional
    public TranscriptAcceptResult acceptTranscript(
            String sessionId,
            String chunkId,
            long sequence,
            String text,
            long startedAtMs,
            long endedAtMs,
            boolean finalChunk
    ) {
        CallSession session = findSessionForUpdate(sessionId);

        // 종료된 세션은 더 이상 STT를 받지 않는다.
        if (!session.isAcceptingTranscript()) {
            throw new BusinessException(ErrorCode.CALL_SESSION_COMPLETED, "종료 중이거나 종료된 통화 세션입니다.");
        }

        long expectedSequence = session.getNextTranscriptSequence();

        /*
         * 기대 sequence보다 작은 값은 대부분 재전송이다.
         * 이 경우 바로 실패시키지 않고, 같은 chunk가 이미 저장되어 있는지 확인해 멱등 처리한다.
         */
        if (sequence < expectedSequence) {
            return handlePossibleDuplicate(sessionId, chunkId, sequence, text, expectedSequence);
        }

        // 기대 sequence보다 크면 중간 청크가 누락된 것이므로 복구가 필요하다.
        if (sequence > expectedSequence) {
            throw sequenceMismatch(expectedSequence);
        }

        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "STT 텍스트는 비어 있을 수 없습니다.");
        }
        if (startedAtMs < 0 || endedAtMs < startedAtMs) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "STT 시간 구간이 올바르지 않습니다.");
        }

        TranscriptChunk chunk = new TranscriptChunk(
                chunkId,
                session,
                sequence,
                text,
                startedAtMs,
                endedAtMs,
                finalChunk
        );
        transcriptChunkRepository.save(chunk);

        /*
         * sequence 증가와 누적 문자 수 갱신은 반드시 DB 저장 이후에만 일어난다.
         * 그래야 "실제로는 저장 실패했는데 ACK만 먼저 나간 상태"를 막을 수 있다.
         */
        session.acceptTranscript(endedAtMs, text.length());

        return new TranscriptAcceptResult(
                chunkId,
                sequence,
                session.getNextTranscriptSequence(),
                false,
                session.getAccumulatedTranscriptCharacters() >= analysisThresholdCharacters
        );
    }

    @Transactional
    public SessionCompletionResult completeSession(String sessionId, OffsetDateTime endedAt, long lastTranscriptSequence) {
        CallSession session = findSessionForUpdate(sessionId);

        /*
         * Flutter가 "마지막으로 보냈다"고 주장하는 sequence와
         * 서버가 실제 저장한 마지막 sequence가 다르면 종료 직전 누락 청크가 있다는 뜻이다.
         */
        long lastStoredSequence = session.getNextTranscriptSequence() - 1;
        if (lastStoredSequence != lastTranscriptSequence) {
            throw sequenceMismatch(session.getNextTranscriptSequence());
        }

        boolean finalAnalysisQueued = false;
        if (session.isAcceptingTranscript()) {
            /*
             * 실제 분석 작업 큐는 아직 단순화되어 있지만,
             * 마지막 분석이 필요하다는 상태 자체는 세션에 남겨 후속 처리 지점을 만든다.
             */
            finalAnalysisQueued = session.queueFinalAnalysisIfNeeded(lastStoredSequence);

            // 세션 종료와 함께 연결된 case도 완료 상태로 정리된다.
            session.complete(endedAt);
        }

        return new SessionCompletionResult(CallSessionResponse.from(session), finalAnalysisQueued);
    }

    private TranscriptAcceptResult handlePossibleDuplicate(
            String sessionId,
            String chunkId,
            long sequence,
            String text,
            long expectedSequence
    ) {
        /*
         * 이미 저장된 sequence가 실제로 존재하는지 먼저 확인한다.
         * 존재하지 않으면 단순 중복이 아니라 클라이언트/서버 상태가 어긋난 것이다.
         */
        TranscriptChunk storedChunk = transcriptChunkRepository.findByCallSessionIdAndSequence(sessionId, sequence)
                .orElseThrow(() -> sequenceMismatch(expectedSequence));

        /*
         * 같은 sequence인데 payload가 다르면 재전송이 아니라 충돌이다.
         * 이 경우는 잘못된 청크가 덮어씌워지는 것을 막기 위해 거부한다.
         */
        if (!storedChunk.hasSamePayload(chunkId, text)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_TRANSCRIPT_CONFLICT,
                    "이미 저장한 sequence에 다른 청크가 수신됐습니다.",
                    Map.of("sequence", sequence)
            );
        }

        return new TranscriptAcceptResult(chunkId, sequence, expectedSequence, true, false);
    }

    private User resolveVictim(CreateCallSessionRequest request) {
        /*
         * 전화번호가 있으면 같은 피해자를 재사용하기 가장 쉬우므로
         * 이름 + 역할 + 전화번호 조합으로 먼저 조회한다.
         */
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            return userRepository.findFirstByNameAndRoleAndPhone(
                            request.victim().name(),
                            UserRole.VICTIM,
                            request.phoneNumber()
                    )
                    .map(existing -> {
                        // 이미 있는 피해자면 최신 나이/전화번호로 보정한다.
                        existing.updateVictimProfile(request.victim().age(), request.phoneNumber());
                        return existing;
                    })
                    .orElseGet(() -> userRepository.save(new User(
                            request.victim().name(),
                            UserRole.VICTIM,
                            request.victim().age(),
                            request.phoneNumber()
                    )));
        }

        // 전화번호가 없으면 완전한 중복 판별이 어렵기 때문에 새 row를 만든다.
        return userRepository.save(new User(
                request.victim().name(),
                UserRole.VICTIM,
                request.victim().age(),
                null
        ));
    }

    private BusinessException sequenceMismatch(long expectedSequence) {
        return new BusinessException(
                ErrorCode.TRANSCRIPT_SEQUENCE_MISMATCH,
                "STT 청크 sequence가 올바르지 않습니다.",
                Map.of("expectedSequence", expectedSequence)
        );
    }

    private CallSession findSession(String sessionId) {
        return callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));
    }

    private CallSession findSessionForUpdate(String sessionId) {
        return callSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));
    }
}
