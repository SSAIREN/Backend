package com.ssairen.backend.domain.notification.dto;

import com.ssairen.backend.domain.casefile.entity.PhishingType;

public record GuardianNotificationCommand(
        Long guardianUserId,
        String guardianFcmToken,
        Long caseId,
        Long victimId,
        PhishingType phishingType,
        String message
) {
}