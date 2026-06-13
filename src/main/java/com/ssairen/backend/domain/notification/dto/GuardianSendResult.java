package com.ssairen.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보호자별 FCM 푸시 발송 결과")
public record GuardianSendResult(
        @Schema(description = "보호자 사용자 ID", example = "2001")
        Long guardianId,

        @Schema(description = "발송 성공 여부", example = "true")
        boolean success
) {
}
