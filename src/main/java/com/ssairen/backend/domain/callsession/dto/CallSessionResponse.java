package com.ssairen.backend.domain.callsession.dto;

import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.CallSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "통화 세션 생성 및 상태 조회 응답")
public record CallSessionResponse(
        @Schema(description = "서버 통화 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "통화 세션 상태", example = "ACTIVE")
        CallSessionStatus status,

        @Schema(description = "서버가 다음으로 기대하는 STT sequence", example = "1")
        long nextTranscriptSequence,

        @Schema(description = "현재까지 저장한 STT 텍스트 누적 글자 수", example = "127")
        long accumulatedTranscriptCharacters,

        @Schema(description = "통화 시작 시각", example = "2026-06-10T15:20:00+09:00")
        OffsetDateTime startedAt,

        @Schema(description = "통화 종료 시각. 진행 중이면 null입니다.", example = "2026-06-10T15:32:00+09:00", nullable = true)
        OffsetDateTime endedAt,

        @Schema(description = "Flutter 피해자 앱 양방향 WebSocket 연결 경로", example = "/ws/v1/victim?sessionId=550e8400-e29b-41d4-a716-446655440000")
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
