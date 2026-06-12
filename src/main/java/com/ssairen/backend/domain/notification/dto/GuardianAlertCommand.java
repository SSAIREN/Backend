package com.ssairen.backend.domain.notification.dto;

import com.ssairen.backend.domain.casefile.entity.PhishingType;
import java.util.List;

public record GuardianAlertCommand(
        Long guardianUserId,
        String guardianName,
        String guardianFcmToken,
        Long victimUserId,
        String victimName,
        String sessionId,
        int riskScore,
        PhishingType phishingType,
        String aiSummary,
        List<String> keywords
) {
}
