package com.ssairen.backend.domain.callsession.api.dto;

import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.CallSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "통화 세션 생성 또는 상태 조회 응답")
public record CallSessionResponse(
        @Schema(description = "서버가 발급한 통화 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "현재 통화 세션 상태", example = "ACTIVE")
        CallSessionStatus status,

        @Schema(description = "서버가 다음으로 기다리는 STT sequence 번호", example = "1")
        long nextTranscriptSequence,

        @Schema(description = "현재까지 누적 저장된 STT 텍스트 길이", example = "127")
        long accumulatedTranscriptCharacters,

        @Schema(description = "통화 시작 시각", example = "2026-06-10T15:20:00+09:00")
        OffsetDateTime startedAt,

        @Schema(description = "통화 종료 시각. 아직 진행 중이면 null입니다.", example = "2026-06-10T15:32:00+09:00", nullable = true)
        OffsetDateTime endedAt,

        @Schema(description = "Flutter가 실시간 감시 단계에서 접속할 WebSocket 경로", example = "/ws/v1/victim?sessionId=550e8400-e29b-41d4-a716-446655440000")
        String webSocketUrl
) {
    public static CallSessionResponse from(CallSession session) {
        return new CallSessionResponse(
                session.getId(),
                session.getStatus(),
                session.getNextTranscriptSequence(),
                session.getAccumulatedTranscriptCharacters(),
                session.getStartedAt(),
                session.getEndedAt(),
                "/ws/v1/victim?sessionId=" + session.getId()
        );
    }
}
