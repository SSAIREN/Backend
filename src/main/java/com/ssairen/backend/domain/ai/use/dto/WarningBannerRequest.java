package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * show_warning_banner tool 콜백 바디다. AI가 피해자 단말에 경고 배너 FCM 표시를 요청한다.
 * 종합 위험도(scenario/riskScore/situationSummary)도 함께 실어 보낸다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WarningBannerRequest(
        String callId,
        String userId,
        String scenario,
        Double riskScore,
        String situationSummary,
        String message
) {
}
