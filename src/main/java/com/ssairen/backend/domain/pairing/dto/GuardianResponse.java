package com.ssairen.backend.domain.pairing.dto;

import com.ssairen.backend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보호자 정보")
public record GuardianResponse(
        @Schema(description = "보호자 사용자 ID", example = "2001")
        Long guardianId,

        @Schema(description = "보호자 이름", example = "김민수")
        String name,

        @Schema(description = "보호자 디바이스의 FCM 토큰", example = "dummy-guardian-fcm-token-2001")
        String fcmToken
) {

    public static GuardianResponse from(User guardian) {
        return new GuardianResponse(guardian.getId(), guardian.getName(), guardian.getFcmToken());
    }
}