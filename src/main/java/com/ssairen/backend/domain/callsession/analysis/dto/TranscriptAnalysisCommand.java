package com.ssairen.backend.domain.callsession.analysis.dto;

public record TranscriptAnalysisCommand(
        String sessionId,
        String chunkId,
        long sequence,
        String chunkTranscript,
        String conversationContext,
        String victimName,
        Integer victimAge,
        String victimPhone
) {
}
