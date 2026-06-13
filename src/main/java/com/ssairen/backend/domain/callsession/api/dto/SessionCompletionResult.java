package com.ssairen.backend.domain.callsession.api.dto;

public record SessionCompletionResult(
        CallSessionResponse response,
        boolean finalAnalysisQueued
) {
}
