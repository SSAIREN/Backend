package com.ssairen.backend.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연령대별 피해자 통계")
public record AgeGroupStatResponse(
        @Schema(description = "연령대", example = "60대 이상")
        String ageGroup,

        @Schema(description = "건수", example = "48")
        long count,

        @Schema(description = "비율(%)", example = "40.0")
        double ratio
) {
}
