package com.ssairen.backend.domain.ai.use.service;

import com.ssairen.backend.domain.ai.use.client.AiDemoClient;
import com.ssairen.backend.domain.ai.use.dto.AiAnalysisCallbackRequest;
import com.ssairen.backend.domain.ai.use.dto.AiDemoRunRequest;
import com.ssairen.backend.domain.ai.use.dto.AiDemoRunResponse;
import com.ssairen.backend.domain.ai.use.dto.AiTriggerRequest;
import com.ssairen.backend.domain.ai.use.dto.AiTriggerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI 데모 오케스트레이션 서비스다. FE 트리거를 FastAPI 데모 호출로 중계하고, AI 종합 결과 콜백을 수신한다.
 */
@Service
public class AiUseService {

    private static final Logger log = LoggerFactory.getLogger(AiUseService.class);

    private final AiDemoClient aiDemoClient;

    public AiUseService(AiDemoClient aiDemoClient) {
        this.aiDemoClient = aiDemoClient;
    }

    public AiTriggerResponse triggerDemo(AiTriggerRequest request) {
        // call_id 를 null 로 보내면 FastAPI 가 새 call_id 를 생성한다.
        AiDemoRunRequest demoRequest = new AiDemoRunRequest(request.message(), null, request.userId());
        AiDemoRunResponse response = aiDemoClient.runDemo(demoRequest);
        return new AiTriggerResponse(response.callId(), response.status());
    }

    public void handleCallback(AiAnalysisCallbackRequest request) {
        log.info("AI 종합 결과 콜백 수신: callId={}, riskLevel={}, riskScore={}",
                request.callId(), request.riskLevel(), request.riskScore());
        // TODO: 추후 FraudCase 갱신 / 대시보드(WebSocket) 통지 / FE 결과 전달 처리. 현재는 수신만 하고 아무 동작 안 함.
    }
}
