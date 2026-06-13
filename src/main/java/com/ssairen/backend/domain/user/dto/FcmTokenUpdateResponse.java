package com.ssairen.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FCM 토큰 등록·갱신 응답")
public record FcmTokenUpdateResponse(
        @Schema(description = "처리 성공 여부", example = "true")
        boolean success
) {
}