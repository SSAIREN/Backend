package com.ssairen.backend.domain.callsession.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Schema(description = "Flutter 통화 모니터링 세션 생성 요청")
public record CreateCallSessionRequest(
        @Schema(description = "MVP 단계에서 Flutter가 하드코딩 또는 더미로 보내는 피해자 userId", example = "1001")
        @NotNull Long userId,

        @Schema(description = "Flutter가 생성한 통화 고유 ID", example = "device-call-001")
        @NotBlank String externalCallId,

        @Schema(description = "Flutter 기기 고유 식별자", example = "victim-device-001")
        @NotBlank String deviceId,

        @Schema(description = "통화 시작 시각", example = "2026-06-10T15:20:00+09:00")
        @NotNull OffsetDateTime startedAt,

        @Schema(description = "상대 또는 피해자 대표 전화번호", example = "01012345678")
        String phoneNumber,

        @Schema(description = "MVP 호환용 피해자 기본 정보. userId 기반 사용자를 보조적으로 갱신할 때 사용")
        @Valid @NotNull VictimRequest victim
) {
    @Schema(description = "피해자 기본 정보")
    public record VictimRequest(
            @Schema(description = "피해자 이름", example = "김OO")
            @NotBlank String name,

            @Schema(description = "피해자 나이", example = "71")
            @NotNull Integer age
    ) {
    }
}
