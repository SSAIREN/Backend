package com.ssairen.backend.domain.callsession.api.dto;

import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.websocket.dto.TranscriptAcceptResult;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "REST STT 분석 결과와 다음 업로드 제어 정보를 함께 반환하는 응답")
public record RestTranscriptAnalysisResponse(
        @Schema(description = "통화 세션 ID")
        String sessionId,

        @Schema(description = "이번에 처리한 STT 청크 ID")
        String chunkId,

        @Schema(description = "서버가 정상적으로 수락한 sequence")
        long acceptedSequence,

        @Schema(description = "서버가 다음으로 기다리는 sequence")
        long nextTranscriptSequence,

        @Schema(description = "이미 처리한 중복 청크인지 여부")
        boolean duplicate,

        @Schema(description = "누적 transcript 길이가 분석 기준치를 넘었는지 여부")
        boolean analysisThresholdReached,

        @Schema(description = "FastAPI가 계산한 보이스피싱 위험 점수", example = "82")
        int riskScore,

        @Schema(description = "판단된 피싱 유형")
        PhishingType phishingType,

        @Schema(description = "FastAPI가 반환한 요약 문장")
        String aiSummary,

        @Schema(description = "탐지 키워드 목록")
        List<String> keywords,

        @Schema(description = "현재 단계에서 Flutter가 WebSocket 실시간 감시로 전환해야 하는지 여부")
        boolean shouldOpenWebSocket
) {
    public static RestTranscriptAnalysisResponse of(
            String sessionId,
            TranscriptAcceptResult acceptResult,
            TranscriptAnalysisResult analysisResult,
            boolean shouldOpenWebSocket
    ) {
        return new RestTranscriptAnalysisResponse(
                sessionId,
                acceptResult.chunkId(),
                acceptResult.acceptedSequence(),
                acceptResult.nextSequence(),
                acceptResult.duplicate(),
                acceptResult.analysisThresholdReached(),
                analysisResult.riskScore(),
                analysisResult.phishingType(),
                analysisResult.aiSummary(),
                analysisResult.keywords(),
                shouldOpenWebSocket
        );
    }
}
