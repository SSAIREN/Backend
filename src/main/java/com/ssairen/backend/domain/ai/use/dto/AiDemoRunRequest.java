package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Backend가 AI(FastAPI)의 POST /api/v1/pipeline-a/runs/demo 로 보내는 요청 바디다.
 * AI 측 PipelineADemoInput(message/call_id/user_id) 필드명에 맞춘다.
 */
public record AiDemoRunRequest(
        String message,
        @JsonProperty("call_id") String callId,
        @JsonProperty("user_id") String userId
) {
}
