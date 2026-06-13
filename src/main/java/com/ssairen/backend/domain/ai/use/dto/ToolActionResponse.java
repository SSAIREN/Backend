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
}
