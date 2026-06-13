package com.ssairen.backend.domain.casefile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경찰 관제 대시보드 상단 요약 지표")
public record CaseSummaryMetricsResponse(
        @Schema(description = "진행중 케이스 수", example = "3")
        long inProgress,

        @Schema(description = "완료된 케이스 수", example = "12")
        long completed,

        @Schema(description = "평균 대응 시간(초). 완료된 케이스가 없으면 null입니다.", example = "245", nullable = true)
        Long avgResponseSec
) {
}
