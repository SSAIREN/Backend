package com.ssairen.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "보호자 FCM 푸시 발송 응답")
public record GuardianNotificationResponse(
        @Schema(description = "보호자별 발송 결과 목록")
        List<GuardianSendResult> sent,

        @Schema(description = "발송에 실패한 보호자 수", example = "0")
        int failCount
) {
}
