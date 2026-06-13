package com.ssairen.backend.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경찰 관제 대시보드 통계 개요")
public record StatsOverviewResponse(
        @Schema(description = "전체 탐지 건수", example = "120")
        long totalDetections,

        @Schema(description = "고위험(riskScore >= 80) 건수", example = "32")
        long highRisk,

        @Schema(description = "지급정지 계좌 수", example = "15")
        long frozenAccounts
) {
}
