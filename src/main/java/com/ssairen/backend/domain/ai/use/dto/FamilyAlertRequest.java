package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * send_family_sms_alert tool 콜백 바디다. AI가 가족 보호자에게 긴급 FCM 알림을 요청한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FamilyAlertRequest(
        String callId,
        String userId,
        String scenario,
        String situationSummary,
        Double riskScore,
        List<String> guardianIds
) {
}
