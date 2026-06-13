package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * send_family_sms_alert tool 콜백 바디다. AI가 가족 보호자에게 긴급 FCM 알림을 요청한다.
 * 보호자(guardianIds)는 sessionId 로 Backend 가 세션에서 해석한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FamilyAlertRequest(
        String sessionId,
        String scenario,
        String situationSummary,
        Double riskScore
) {
}
