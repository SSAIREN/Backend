package com.ssairen.backend.domain.casefile.dto;

import com.ssairen.backend.domain.casefile.entity.CaseStatus;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "경찰 관제 대시보드 케이스 목록 항목")
public record CaseSummaryResponse(
        @Schema(description = "케이스 ID", example = "1")
        Long caseId,

        @Schema(description = "피해자 이름", example = "김OO")
        String victimName,

        @Schema(description = "피해자 나이", example = "71")
        Integer age,

        @Schema(description = "위험도 점수", example = "85")
        Integer riskScore,

        @Schema(description = "보이스피싱 유형", example = "KIDNAPPING_THREAT", nullable = true)
        PhishingType phishingType,

        @Schema(description = "케이스 진행 상태", example = "IN_PROGRESS")
        CaseStatus status,

        @Schema(description = "탐지된 주요 키워드", example = "검찰, 계좌 이체, 안전 계좌", nullable = true)
        String keywords,

        @Schema(description = "지역", example = "서울특별시 강남구", nullable = true)
        String region,

        @Schema(description = "통화 길이(초)", example = "180", nullable = true)
        Integer callDurationSec,

        @Schema(description = "탐지 시각", example = "2026-06-10T15:20:00+09:00")
        OffsetDateTime detectedAt
) {
    public static CaseSummaryResponse from(FraudCase fraudCase) {
        return new CaseSummaryResponse(
                fraudCase.getId(),
                fraudCase.getVictim().getName(),
                fraudCase.getVictim().getAge(),
                fraudCase.getRiskScore(),
                fraudCase.getPhishingType(),
                fraudCase.getStatus(),
                fraudCase.getKeywords(),
                fraudCase.getRegion(),
                fraudCase.getCallDurationSec(),
                fraudCase.getDetectedAt()
        );
    }
}