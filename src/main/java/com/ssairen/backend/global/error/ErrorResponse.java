package com.ssairen.backend.global.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;

@Schema(description = "공통 오류 응답")
public record ErrorResponse(
        @Schema(description = "서버 오류 코드", example = "CALL_SESSION_NOT_FOUND")
        String code,

        @Schema(description = "사용자 또는 개발자 확인용 오류 메시지", example = "통화 세션을 찾을 수 없습니다.")
        String message,

        @Schema(description = "오류 발생 시각", example = "2026-06-10T15:20:47+09:00")
        OffsetDateTime timestamp,

        @Schema(description = "sequence 기대값 등 오류별 추가 정보")
        Map<String, Object> details
) {
    public static ErrorResponse from(BusinessException exception) {
        return new ErrorResponse(
                exception.getErrorCode().name(),
                exception.getMessage(),
                OffsetDateTime.now(),
                exception.getDetails()
        );
    }
}
