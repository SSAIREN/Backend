package com.ssairen.backend.domain.callsession.analysis;

import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.TranscriptChunk;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.callsession.repository.TranscriptChunkRepository;
import com.ssairen.backend.domain.casefile.service.DashboardNotificationService;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import com.ssairen.backend.global.logging.DebugExecutionTimer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TranscriptAnalysisService {

    private final CallSessionRepository callSessionRepository;
    private final TranscriptChunkRepository transcriptChunkRepository;
    private final TranscriptAnalysisGateway transcriptAnalysisGateway;
    private final DashboardNotificationService dashboardNotificationService;
    private final int dashboardNewCaseRiskScore;

    public TranscriptAnalysisService(
            CallSessionRepository callSessionRepository,
            TranscriptChunkRepository transcriptChunkRepository,
            TranscriptAnalysisGateway transcriptAnalysisGateway,
            DashboardNotificationService dashboardNotificationService,
            @Value("${ssairen.analysis.dashboard-new-case-risk-score:76}") int dashboardNewCaseRiskScore
    ) {
        this.callSessionRepository = callSessionRepository;
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.transcriptAnalysisGateway = transcriptAnalysisGateway;
        this.dashboardNotificationService = dashboardNotificationService;
        this.dashboardNewCaseRiskScore = dashboardNewCaseRiskScore;
    }

    @Transactional
    public TranscriptAnalysisResult analyzeRestChunk(String sessionId, String chunkId, long sequence) {
        return analyze(sessionId, chunkId, sequence, true);
    }

    @Transactional
    public TranscriptAnalysisResult analyzeWebSocketChunk(String sessionId, String chunkId, long sequence) {
        return analyze(sessionId, chunkId, sequence, false);
    }

    private TranscriptAnalysisResult analyze(String sessionId, String chunkId, long sequence, boolean restFlow) {
        CallSession session = DebugExecutionTimer.measure(
                        log,
                        "db-read",
                        "callSessionRepository.findByIdForUpdate",
                        "sessionId=" + sessionId,
                        () -> callSessionRepository.findByIdForUpdate(sessionId)
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "Call session not found."));

        TranscriptContext transcriptContext = buildTranscriptContext(sessionId, chunkId, sequence);

        TranscriptAnalysisCommand command = new TranscriptAnalysisCommand(
                sessionId,
                chunkId,
                sequence,
                transcriptContext.chunkTranscript(),
                transcriptContext.conversationContext(),
                session.getVictim().getName(),
                session.getVictim().getAge(),
                session.getVictim().getPhone()
        );

        TranscriptAnalysisResult result = restFlow
                ? transcriptAnalysisGateway.analyzeRest(command)
                : transcriptAnalysisGateway.analyzeWebSocket(command);

        DebugExecutionTimer.measure(
                log,
                "db-update",
                "fraudCase.applyAnalysisResult",
                "sessionId=" + sessionId + ", chunkId=" + chunkId + ", sequence=" + sequence,
                () -> session.getFraudCase().applyAnalysisResult(
                        result.riskScore(),
                        result.phishingType(),
                        result.aiSummary(),
                        result.keywords()
                )
        );

        notifyDashboardIfAnomalyDetected(session, result);
        return result;
    }

    private void notifyDashboardIfAnomalyDetected(CallSession session, TranscriptAnalysisResult result) {
        if (result.riskScore() < dashboardNewCaseRiskScore) {
            return;
        }
        if (!session.markDashboardCaseNotifiedIfNeeded()) {
            return;
        }
        dashboardNotificationService.broadcastNewCase(session.getFraudCase());
    }

    private TranscriptContext buildTranscriptContext(String sessionId, String chunkId, long sequence) {
        List<TranscriptChunk> chunks = DebugExecutionTimer.measure(
                log,
                "db-read",
                "transcriptChunkRepository.findAllByCallSessionIdAndSequenceLessThanEqualOrderBySequenceAsc",
                "sessionId=" + sessionId + ", sequence<=" + sequence,
                () -> transcriptChunkRepository.findAllByCallSessionIdAndSequenceLessThanEqualOrderBySequenceAsc(sessionId, sequence)
        );

        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "No transcript chunks are stored for analysis.");
        }

        Map<String, StringJoiner> groupedByChunkId = new LinkedHashMap<>();
        for (TranscriptChunk chunk : chunks) {
            groupedByChunkId
                    .computeIfAbsent(chunk.getChunkId(), ignored -> new StringJoiner(" "))
                    .add(chunk.getText().trim());
        }

        StringJoiner chunkTranscriptJoiner = groupedByChunkId.get(chunkId);
        if (chunkTranscriptJoiner == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Requested chunkId was not found in stored transcripts.",
                    Map.of("chunkId", chunkId, "sequence", sequence)
            );
        }

        String conversationContext = groupedByChunkId.values().stream()
                .map(StringJoiner::toString)
                .filter(text -> !text.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        return new TranscriptContext(chunkTranscriptJoiner.toString(), conversationContext);
    }

    private record TranscriptContext(String chunkTranscript, String conversationContext) {
    }
}
