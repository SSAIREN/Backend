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

    /*
     * Flutter transcript 업로드는 두 단계로 처리된다.
     * 1. 먼저 transcript를 저장하고 sequence를 검증한다.
     * 2. 그 다음 FastAPI에 분석을 요청해 결과를 Flutter에게 돌려준다.
     * 이 오케스트레이션을 컨트롤러가 아니라 별도 facade 서비스에 모아두면
     * REST 흐름과 WebSocket 흐름이 공통 규칙을 더 쉽게 공유할 수 있다.
     */
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
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "REST 분석 API는 확정된 STT 청크만 허용합니다.");
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
                request.sequence(),
                request.text()
        );

        /*
         * REST 단계에서는 Flutter가 이 값만 보고
         * "아직 일반 모드 유지" 또는 "이제 실시간 모니터링용 WebSocket 연결" 여부를 결정할 수 있다.
         */
        boolean shouldOpenWebSocket = analysisResult.riskScore() >= websocketEscalationThreshold;

        return RestTranscriptAnalysisResponse.of(sessionId, acceptResult, analysisResult, shouldOpenWebSocket);
    }
}
