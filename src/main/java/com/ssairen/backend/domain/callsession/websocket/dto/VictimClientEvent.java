package com.ssairen.backend.domain.callsession.websocket.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Flutter -> Spring Boot WebSocket 공통 이벤트 envelope")
public record VictimClientEvent(
        @Schema(description = "이벤트 고유 ID", example = "event-001")
        String eventId,

        @Schema(description = "이벤트 종류", allowableValues = {"TRANSCRIPT_CHUNK", "SESSION_COMPLETE", "PING"}, example = "TRANSCRIPT_CHUNK")
        String eventType,

        @Schema(description = "통화 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "Flutter 이벤트 생성 시각", example = "2026-06-10T15:20:47+09:00")
        OffsetDateTime occurredAt,

        @Schema(description = "eventType별 payload")
        JsonNode data
) {
}
