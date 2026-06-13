package com.ssairen.backend.domain.casefile.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경찰 대시보드 WebSocket - 신규 케이스 발생 이벤트")
public record NewCaseEvent(
        @Schema(description = "이벤트 종류", example = "NEW_CASE")
        String type,

        @Schema(description = "신규 케이스 정보")
        @JsonProperty("case")
        CaseSummaryResponse caseSummary
) {
    public static NewCaseEvent of(CaseSummaryResponse caseSummary) {
        return new NewCaseEvent("NEW_CASE", caseSummary);
    }
}