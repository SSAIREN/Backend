package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * save_evidence tool 콜백 바디다. AI가 통화 내용을 증거로 저장하도록 요청한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvidenceRequest(
        String sessionId,
        String conversationText,
        Double riskScore,
        String scenario
) {
}
