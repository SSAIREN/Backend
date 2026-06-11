package com.ssairen.backend.domain.callsession.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "SESSION_COMPLETE 이벤트의 data")
public record SessionCompletePayload(
        @Schema(description = "통화 종료 시각", example = "2026-06-10T15:32:00+09:00")
        OffsetDateTime endedAt,

        @Schema(description = "Flutter가 전송한 마지막 STT sequence", example = "42")
        long lastTranscriptSequence
) {
}
