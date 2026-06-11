package com.ssairen.backend.domain.callsession.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Flutter가 5초 단위 STT 텍스트를 REST로 업로드할 때 사용하는 요청")
public record RestTranscriptAnalysisRequest(
        @Schema(description = "Flutter가 생성한 STT 청크 고유 ID", example = "chunk-001")
        @NotBlank String chunkId,

        @Schema(description = "통화 세션 내 STT 순번", example = "1")
        @Min(1) long sequence,

        @Schema(description = "확정된 STT 텍스트", example = "검찰 수사관입니다. 안전계좌로 이체하세요.")
        @NotBlank String text,

        @Schema(description = "통화 시작 기준 청크 시작 밀리초", example = "0")
        @Min(0) long startedAtMs,

        @Schema(description = "통화 시작 기준 청크 종료 밀리초", example = "3000")
        @Min(0) long endedAtMs,

        @Schema(description = "현재 서버는 확정된 STT 청크만 허용한다.", example = "true")
        boolean isFinal
) {
}
