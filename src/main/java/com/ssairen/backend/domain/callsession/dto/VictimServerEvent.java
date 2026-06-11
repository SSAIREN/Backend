package com.ssairen.backend.domain.callsession.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Spring Boot -> Flutter WebSocket 공통 이벤트 envelope")
public record VictimServerEvent(
        @Schema(description = "서버 이벤트 고유 ID", example = "server-event-001")
        String eventId,

        @Schema(
                description = "서버 이벤트 종류",
                allowableValues = {"SESSION_READY", "TRANSCRIPT_ACK", "TRANSCRIPT_NACK", "SESSION_COMPLETE_ACK", "PONG"},
                example = "TRANSCRIPT_ACK"
        )
        String eventType,

        @Schema(description = "통화 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "서버 이벤트 생성 시각", example = "2026-06-10T15:20:47+09:00")
        OffsetDateTime occurredAt,

        @Schema(description = "eventType별 payload")
        Object data
) {
    public static VictimServerEvent of(String eventType, String sessionId, Object data) {
        return new VictimServerEvent(
                UUID.randomUUID().toString(),
                eventType,
                sessionId,
                OffsetDateTime.now(),
                data
        );
    }
}
