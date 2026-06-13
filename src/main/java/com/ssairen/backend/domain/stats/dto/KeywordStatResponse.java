package com.ssairen.backend.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "키워드별 탐지 빈도 통계")
public record KeywordStatResponse(
        @Schema(description = "순위", example = "1")
        int rank,

        @Schema(description = "키워드", example = "안전 계좌")
        String keyword,

        @Schema(description = "탐지 횟수", example = "24")
        long count
) {
}
