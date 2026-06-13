package com.ssairen.backend.domain.ai.use.dto;

/**
 * FE 트리거 요청을 접수했음을 알리는 즉시 응답이다.
 * 실제 분석 결과는 AI가 비동기 처리 후 /ai/use/callback 으로 push 한다.
 */
public record AiTriggerResponse(
        String callId,
        String status
) {
}
