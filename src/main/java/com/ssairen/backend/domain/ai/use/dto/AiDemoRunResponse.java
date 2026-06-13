package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 데모 작업 접수 응답(202)이다. AI는 call_id 와 폴링용 status_url 을 즉시 반환한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiDemoRunResponse(
        @JsonProperty("call_id") String callId,
        String status,
        @JsonProperty("status_url") String statusUrl
) {
}
