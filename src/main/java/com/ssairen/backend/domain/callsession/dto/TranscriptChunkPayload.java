package com.ssairen.backend.domain.callsession.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TRANSCRIPT_CHUNK 이벤트의 data")
public record TranscriptChunkPayload(
        @Schema(description = "Flutter가 생성한 STT 청크 고유 ID", example = "chunk-001")
        String chunkId,

        @Schema(description = "통화 세션 내 STT 순번. 1부터 순차 증가합니다.", example = "1")
        long sequence,

        @Schema(description = "Flutter STT 엔진이 확정한 텍스트", example = "검찰 수사관입니다.")
        String text,

        @Schema(description = "통화 시작 기준 청크 시작 밀리초", example = "0")
        long startedAtMs,

        @Schema(description = "통화 시작 기준 청크 종료 밀리초", example = "3000")
        long endedAtMs,

        @Schema(description = "확정 STT 여부. 현재 서버는 true만 허용합니다.", example = "true")
        boolean isFinal
) {
}
