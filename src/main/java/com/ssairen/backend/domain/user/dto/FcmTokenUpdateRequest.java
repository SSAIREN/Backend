package com.ssairen.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "FCM 토큰 등록·갱신 요청")
public record FcmTokenUpdateRequest(
        @Schema(description = "디바이스에서 발급받은 FCM 토큰", example = "dummy-fcm-token-abc123")
        @NotBlank
        String fcmToken
) {
}