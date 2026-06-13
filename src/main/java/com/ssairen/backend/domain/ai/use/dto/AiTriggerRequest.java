package com.ssairen.backend.domain.ai.use.dto;

/**
 * FE가 위험 트리거 이후 Backend로 보내는 데모 분석 요청이다.
 * Backend는 이 요청을 받아 AI(FastAPI) 데모 파이프라인을 비동기로 호출한다.
 */
public record AiTriggerRequest(
        String sessionId,
        String message
) {
}
