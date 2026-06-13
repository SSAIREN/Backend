package com.ssairen.backend.domain.casefile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "케이스 상태 변경 응답")
public record CaseStatusUpdateResponse(
        @Schema(description = "처리 성공 여부", example = "true")
        boolean success
) {
}
