package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * AI가 데모 파이프라인 처리를 끝낸 뒤 Backend로 push 하는 최종 분석 결과다.
 * (AI -> Backend, 비동기 콜백)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisCallbackRequest(
        String sessionId,
        String detectedScenario,
        String riskLevel,
        Double riskScore,
        List<String> detectedKeywords,
        String situationSummary,
        List<String> toolsToCall,
        List<String> finalActionsTaken,
        String response
) {
}
