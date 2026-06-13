package com.ssairen.backend.domain.guardianreply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "보호자가 피해자 앱으로 보내는 응답 요청")
public record GuardianReplyRequest(
        @Schema(description = "응답하는 보호자 사용자 ID", example = "1001")
        @NotNull
        Long guardianId,

        @Schema(description = "보호자 현재 위도", example = "37.4979")
        @NotNull
        Double latitude,

        @Schema(description = "보호자 현재 경도", example = "127.0276")
        @NotNull
        Double longitude,

        @Schema(description = "보호자가 입력한 메시지", example = "저 안전해요, 그 전화 끊으세요")
        @NotBlank
        String message
) {
}
