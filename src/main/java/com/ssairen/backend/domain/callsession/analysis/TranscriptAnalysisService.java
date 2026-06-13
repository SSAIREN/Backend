package com.ssairen.backend.domain.callsession.analysis;

import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscriptAnalysisService {

    /*
     * 이 서비스는 transcript 청크를 FastAPI로 보내고,
     * 응답으로 받은 최신 위험도와 메타데이터를 사건 엔티티에 반영한다.
     * Flutter가 REST로 보내는지, WebSocket으로 보내는지에 따라
     * FastAPI의 서로 다른 endpoint를 호출하는 것이 핵심 책임이다.
     */
    private final CallSessionRepository callSessionRepository;
    private final TranscriptAnalysisGateway transcriptAnalysisGateway;

    public TranscriptAnalysisService(
            CallSessionRepository callSessionRepository,
            TranscriptAnalysisGateway transcriptAnalysisGateway
    ) {
        this.callSessionRepository = callSessionRepository;
        this.transcriptAnalysisGateway = transcriptAnalysisGateway;
    }

    @Transactional
    public TranscriptAnalysisResult analyzeRestChunk(String sessionId, long sequence, String transcript) {
        return analyze(sessionId, sequence, transcript, true);
    }

    @Transactional
    public TranscriptAnalysisResult analyzeWebSocketChunk(String sessionId, long sequence, String transcript) {
        return analyze(sessionId, sequence, transcript, false);
    }

    private TranscriptAnalysisResult analyze(String sessionId, long sequence, String transcript, boolean restFlow) {
        CallSession session = callSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));

        TranscriptAnalysisCommand command = new TranscriptAnalysisCommand(
                sessionId,
                sequence,
                transcript,
                session.getVictim().getName(),
                session.getVictim().getAge(),
                session.getVictim().getPhone()
        );

        TranscriptAnalysisResult result = restFlow
                ? transcriptAnalysisGateway.analyzeRest(command)
                : transcriptAnalysisGateway.analyzeWebSocket(command);

        /*
         * 사건 엔티티에는 항상 가장 최근 분석 결과를 반영한다.
         * 이후 Flutter가 REST 응답이든 WebSocket 이벤트든 무엇을 보더라도
         * DB 기준 최신 위험 상태와 일관된 값을 확인할 수 있게 만들기 위함이다.
         */
        session.getFraudCase().applyAnalysisResult(
                result.riskScore(),
                result.phishingType(),
                result.aiSummary(),
                result.keywords()
        );

        return result;
    }
}
