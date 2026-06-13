package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Backend가 AI(FastAPI)의 POST /api/v1/pipeline-a/runs/demo 로 보내는 요청 바디다.
 * AI 측 데모 입력(message/session_id) 필드명에 맞춘다.
 */
public record AiDemoRunRequest(
        String message,
        @JsonProperty("session_id") String sessionId
) {
}
