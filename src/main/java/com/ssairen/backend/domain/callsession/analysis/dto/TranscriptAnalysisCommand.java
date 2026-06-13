package com.ssairen.backend.domain.callsession.analysis.dto;

public record TranscriptAnalysisCommand(
        String sessionId,
        long sequence,
        String transcript,
        String victimName,
        Integer victimAge,
        String victimPhone
) {
}
