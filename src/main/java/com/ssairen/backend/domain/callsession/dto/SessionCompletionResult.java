package com.ssairen.backend.domain.callsession.dto;

public record SessionCompletionResult(
        CallSessionResponse response,
        boolean finalAnalysisQueued
) {
}
