package com.ssairen.backend.domain.notification.dto;

import com.ssairen.backend.domain.casefile.entity.PhishingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "보호자 FCM 푸시 발송 요청")
public record GuardianNotificationRequest(
        @Schema(description = "알림과 연관된 케이스 ID", example = "1")
        @NotNull
        Long caseId,

        @Schema(description = "피해자 사용자 ID", example = "1001")
        @NotNull
        Long victimId,

        @Schema(description = "보이스피싱 유형", example = "AGENCY_IMPERSONATION")
        @NotNull
        PhishingType phishingType,

        @Schema(description = "보호자에게 전달할 알림 메시지", example = "검찰 사칭형 보이스피싱이 의심됩니다.")
        @NotBlank
        String message
) {
}
