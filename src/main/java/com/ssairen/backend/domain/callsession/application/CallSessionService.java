package com.ssairen.backend.domain.callsession.application;

import com.ssairen.backend.domain.callsession.api.dto.CallSessionResponse;
import com.ssairen.backend.domain.callsession.api.dto.CreateCallSessionRequest;
import com.ssairen.backend.domain.callsession.api.dto.SessionCompletionResult;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.TranscriptChunk;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.callsession.repository.TranscriptChunkRepository;
import com.ssairen.backend.domain.callsession.websocket.dto.TranscriptAcceptResult;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.casefile.service.DashboardNotificationService;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import com.ssairen.backend.global.logging.DebugExecutionTimer;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CallSessionService {

    private final CallSessionRepository callSessionRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final UserRepository userRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final DashboardNotificationService dashboardNotificationService;
    private final long analysisThresholdCharacters;

    public CallSessionService(
            CallSessionRepository callSessionRepository,
            TranscriptChunkRepository transcriptChunkRepository,
            UserRepository userRepository,
            FraudCaseRepository fraudCaseRepository,
            DashboardNotificationService dashboardNotificationService,
            @Value("${ssairen.transcript.analysis-threshold-characters:1000}") long analysisThresholdCharacters
    ) {
        this.callSessionRepository = callSessionRepository;
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.userRepository = userRepository;
        this.fraudCaseRepository = fraudCaseRepository;
        this.dashboardNotificationService = dashboardNotificationService;
        this.analysisThresholdCharacters = analysisThresholdCharacters;
    }

    @Transactional
    public CallSessionResponse createSession(CreateCallSessionRequest request) {
        return DebugExecutionTimer.measure(
                        log,
                        "db-read",
                        "callSessionRepository.findByExternalCallId",
                        "externalCallId=" + request.externalCallId(),
                        () -> callSessionRepository.findByExternalCallId(request.externalCallId())
                )
                .map(CallSessionResponse::from)
                .orElseGet(() -> {
                    User victim = resolveVictim(request);
                    FraudCase fraudCase = DebugExecutionTimer.measure(
                            log,
                            "db-write",
                            "fraudCaseRepository.save",
                            "victimId=" + victim.getId(),
                            () -> fraudCaseRepository.save(new FraudCase(victim, request.startedAt()))
                    );
                    dashboardNotificationService.broadcastNewCase(fraudCase);

                    CallSession session = new CallSession(
                            UUID.randomUUID().toString(),
                            request.externalCallId(),
                            request.deviceId(),
                            victim,
                            fraudCase,
                            request.startedAt()
                    );

                    return CallSessionResponse.from(DebugExecutionTimer.measure(
                            log,
                            "db-write",
                            "callSessionRepository.save",
                            "sessionId=" + session.getId() + ", externalCallId=" + request.externalCallId(),
                            () -> callSessionRepository.save(session)
                    ));
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

        if (!session.isAcceptingTranscript()) {
            throw new BusinessException(ErrorCode.CALL_SESSION_COMPLETED, "종료 중이거나 이미 종료된 통화 세션입니다.");
        }

        long expectedSequence = session.getNextTranscriptSequence();

        if (sequence < expectedSequence) {
            return handlePossibleDuplicate(sessionId, chunkId, sequence, text, expectedSequence);
        }

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
        DebugExecutionTimer.measure(
                log,
                "db-write",
                "transcriptChunkRepository.save",
                "sessionId=" + sessionId + ", chunkId=" + chunkId + ", sequence=" + sequence,
                () -> transcriptChunkRepository.save(chunk)
        );

        DebugExecutionTimer.measure(
                log,
                "db-update",
                "callSession.acceptTranscript",
                "sessionId=" + sessionId + ", sequence=" + sequence,
                () -> session.acceptTranscript(endedAtMs, text.length())
        );

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

        long lastStoredSequence = session.getNextTranscriptSequence() - 1;
        if (lastStoredSequence != lastTranscriptSequence) {
            throw sequenceMismatch(session.getNextTranscriptSequence());
        }

        boolean finalAnalysisQueued = false;
        if (session.isAcceptingTranscript()) {
            finalAnalysisQueued = DebugExecutionTimer.measure(
                    log,
                    "db-update",
                    "callSession.queueFinalAnalysisIfNeeded",
                    "sessionId=" + sessionId + ", lastStoredSequence=" + lastStoredSequence,
                    () -> session.queueFinalAnalysisIfNeeded(lastStoredSequence)
            );
            DebugExecutionTimer.measure(
                    log,
                    "db-update",
                    "callSession.complete",
                    "sessionId=" + sessionId,
                    () -> session.complete(endedAt)
            );
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
        TranscriptChunk storedChunk = DebugExecutionTimer.measure(
                        log,
                        "db-read",
                        "transcriptChunkRepository.findByCallSessionIdAndSequence",
                        "sessionId=" + sessionId + ", sequence=" + sequence,
                        () -> transcriptChunkRepository.findByCallSessionIdAndSequence(sessionId, sequence)
                )
                .orElseThrow(() -> sequenceMismatch(expectedSequence));

        if (!storedChunk.hasSamePayload(chunkId, text)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_TRANSCRIPT_CONFLICT,
                    "이미 수신한 sequence에 다른 chunk payload가 들어왔습니다.",
                    Map.of("sequence", sequence)
            );
        }

        return new TranscriptAcceptResult(chunkId, sequence, expectedSequence, true, false);
    }

    private User resolveVictim(CreateCallSessionRequest request) {
        User victim = DebugExecutionTimer.measure(
                        log,
                        "db-read",
                        "userRepository.findByIdAndRole",
                        "userId=" + request.userId() + ", role=" + UserRole.VICTIM,
                        () -> userRepository.findByIdAndRole(request.userId(), UserRole.VICTIM)
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "MVP 플로우를 위해 userId를 찾을 수 없습니다.",
                        Map.of("userId", request.userId())
                ));

        DebugExecutionTimer.measure(
                log,
                "db-update",
                "victim.updateVictimProfile",
                "userId=" + victim.getId(),
                () -> victim.updateVictimProfile(request.victim().age(), request.phoneNumber())
        );
        return victim;
    }

    private BusinessException sequenceMismatch(long expectedSequence) {
        return new BusinessException(
                ErrorCode.TRANSCRIPT_SEQUENCE_MISMATCH,
                "STT 청크 sequence가 올바르지 않습니다.",
                Map.of("expectedSequence", expectedSequence)
        );
    }

    private CallSession findSession(String sessionId) {
        return DebugExecutionTimer.measure(
                        log,
                        "db-read",
                        "callSessionRepository.findById",
                        "sessionId=" + sessionId,
                        () -> callSessionRepository.findById(sessionId)
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));
    }

    private CallSession findSessionForUpdate(String sessionId) {
        return DebugExecutionTimer.measure(
                        log,
                        "db-read",
                        "callSessionRepository.findByIdForUpdate",
                        "sessionId=" + sessionId,
                        () -> callSessionRepository.findByIdForUpdate(sessionId)
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));
    }
}
