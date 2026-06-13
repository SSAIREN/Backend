package com.ssairen.backend.domain.casefile.dto;

import com.ssairen.backend.domain.responseaction.entity.ResponseAction;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionStatus;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "케이스 대응 조치 항목")
public record ResponseActionResponse(
        @Schema(description = "대응 조치 유형", example = "GPS")
        ResponseActionType actionType,

        @Schema(description = "대응 조치 상태", example = "COMPLETED")
        ResponseActionStatus status,

        @Schema(description = "대응 조치 결과", example = "보호자 위치 조회 완료", nullable = true)
        String result,

        @Schema(description = "실행 시각", example = "2026-06-10T15:21:00+09:00", nullable = true)
        OffsetDateTime executedAt
) {
    public static ResponseActionResponse from(ResponseAction action) {
        return new ResponseActionResponse(
                action.getActionType(),
                action.getStatus(),
                action.getResult(),
                action.getExecutedAt()
        );
    }
}
