package com.ssairen.backend.domain.ai.use.dto;

/**
 * AI tool 콜백에 대한 공통 응답이다. AI 측 tool client 는 status 값을 보고 성공 여부를 판단한다.
 */
public record ToolActionResponse(
        String tool,
        String status,
        String message
) {
    public static ToolActionResponse ok(String tool) {
        return new ToolActionResponse(tool, "SUCCESS", null);
    }

    public static ToolActionResponse ok(String tool, String message) {
        return new ToolActionResponse(tool, "SUCCESS", message);
    }

    /**
     * 세션당 한 번만 수행되는 tool 이 이미 수행되어 중복 호출을 건너뛴 경우의 응답이다.
     */
    public static ToolActionResponse skipped(String tool, String message) {
        return new ToolActionResponse(tool, "SKIPPED", message);
    }
}
