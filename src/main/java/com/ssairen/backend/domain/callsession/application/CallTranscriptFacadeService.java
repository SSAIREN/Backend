package com.ssairen.backend.domain.callsession.application;

import com.ssairen.backend.domain.callsession.analysis.TranscriptAnalysisService;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.api.dto.RestTranscriptAnalysisRequest;
import com.ssairen.backend.domain.callsession.api.dto.RestTranscriptAnalysisResponse;
import com.ssairen.backend.domain.callsession.websocket.dto.TranscriptAcceptResult;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CallTranscriptFacadeService {

    private final CallSessionService callSessionService;
    private final TranscriptAnalysisService transcriptAnalysisService;
    private final int websocketEscalationThreshold;

    public CallTranscriptFacadeService(
            CallSessionService callSessionService,
            TranscriptAnalysisService transcriptAnalysisService,
            @Value("${ssairen.analysis.websocket-escalation-threshold:70}") int websocketEscalationThreshold
    ) {
        this.callSessionService = callSessionService;
        this.transcriptAnalysisService = transcriptAnalysisService;
        this.websocketEscalationThreshold = websocketEscalationThreshold;
    }

    public RestTranscriptAnalysisResponse analyzeTranscriptViaRest(
            String sessionId,
            RestTranscriptAnalysisRequest request
    ) {
        if (!request.isFinal()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "REST analysis API only accepts final STT chunks.");
        }

        TranscriptAcceptResult acceptResult = callSessionService.acceptTranscript(
                sessionId,
                request.chunkId(),
                request.sequence(),
                request.text(),
                request.startedAtMs(),
                request.endedAtMs(),
                request.isFinal()
        );

        TranscriptAnalysisResult analysisResult = transcriptAnalysisService.analyzeRestChunk(
                sessionId,
                request.chunkId(),
                request.sequence()
        );

        boolean shouldOpenWebSocket = analysisResult.riskScore() >= websocketEscalationThreshold;
        return RestTranscriptAnalysisResponse.of(sessionId, acceptResult, analysisResult, shouldOpenWebSocket);
    }
}
