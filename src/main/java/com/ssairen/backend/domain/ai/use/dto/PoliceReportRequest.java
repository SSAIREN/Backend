package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * notify_police tool 콜백 바디다. AI가 경찰 신고를 요청한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PoliceReportRequest(
        String callId,
        String userId,
        Integer riskScore,
        String incidentType,
        String summary
) {
}
