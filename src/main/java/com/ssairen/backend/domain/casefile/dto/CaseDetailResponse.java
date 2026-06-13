package com.ssairen.backend.domain.casefile.dto;

import com.ssairen.backend.domain.casefile.entity.FraudCase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "경찰 관제 대시보드 케이스 상세")
public record CaseDetailResponse(
        @Schema(description = "케이스 ID", example = "1")
        Long caseId,

        @Schema(description = "AI 통화 분석 요약", example = "검찰 수사관을 사칭하여 계좌 이체를 유도함", nullable = true)
        String aiSummary,

        @Schema(description = "탐지된 주요 키워드", example = "검찰, 계좌 이체, 안전 계좌", nullable = true)
        String keywords,

        @Schema(description = "위도", example = "37.4979", nullable = true)
        BigDecimal latitude,

        @Schema(description = "경도", example = "127.0276", nullable = true)
        BigDecimal longitude,

        @Schema(description = "대응 프로세스 진행 상황")
        List<ResponseActionResponse> actions
) {
    public static CaseDetailResponse from(FraudCase fraudCase, List<ResponseActionResponse> actions) {
        return new CaseDetailResponse(
                fraudCase.getId(),
                fraudCase.getAiSummary(),
                fraudCase.getKeywords(),
                fraudCase.getLatitude(),
                fraudCase.getLongitude(),
                actions
        );
    }
}
