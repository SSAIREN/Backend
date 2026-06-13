package com.ssairen.backend.domain.casefile.websocket.dto;

import com.ssairen.backend.domain.responseaction.entity.ResponseActionStatus;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경찰 대시보드 WebSocket - 케이스 대응 조치 진행상황 변경 이벤트")
public record ActionUpdateEvent(
        @Schema(description = "이벤트 종류", example = "ACTION_UPDATE")
        String type,

        @Schema(description = "케이스 ID", example = "1")
        Long caseId,

        @Schema(description = "대응 조치 종류", example = "GPS")
        ResponseActionType actionType,

        @Schema(description = "대응 조치 진행상황", example = "COMPLETED")
        ResponseActionStatus status
) {
    public static ActionUpdateEvent of(Long caseId, ResponseActionType actionType, ResponseActionStatus status) {
        return new ActionUpdateEvent("ACTION_UPDATE", caseId, actionType, status);
    }
}