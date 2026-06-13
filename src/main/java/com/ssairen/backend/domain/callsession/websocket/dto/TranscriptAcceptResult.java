package com.ssairen.backend.domain.callsession.websocket.dto;

public record TranscriptAcceptResult(
        String chunkId,
        long acceptedSequence,
        long nextSequence,
        boolean duplicate,
        boolean analysisThresholdReached
) {
}
