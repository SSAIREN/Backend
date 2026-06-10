package com.ssairen.backend.domain.callsession.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Schema(description = "Flutter 통화 모니터링 세션 생성 요청")
public record CreateCallSessionRequest(
        @Schema(description = "Flutter가 생성한 통화 고유 ID. 동일 통화 재요청의 멱등 키로 사용합니다.", example = "device-call-001")
        @NotBlank String externalCallId,

        @Schema(description = "Flutter 기기 고유 식별자", example = "victim-device-001")
        @NotBlank String deviceId,

        @Schema(description = "통화 시작 시각", example = "2026-06-10T15:20:00+09:00")
        @NotNull OffsetDateTime startedAt,

        @Schema(description = "상대방 전화번호. 서버 로그에는 원문을 남기지 않습니다.", example = "01012345678")
        String phoneNumber,

        @Schema(description = "피해자 기본 정보")
        @Valid @NotNull VictimRequest victim
) {
        @Schema(description = "피해자 기본 정보")
        public record VictimRequest(
                @Schema(description = "피해자 마스킹 이름", example = "김OO")
                @NotBlank String name,

                @Schema(description = "피해자 나이", example = "71")
                @NotNull Integer age
        ) {
        }
}
