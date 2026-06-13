package com.ssairen.backend.domain.ai.use.controller;

import com.ssairen.backend.domain.ai.use.dto.AiAnalysisCallbackRequest;
import com.ssairen.backend.domain.ai.use.dto.AiTriggerRequest;
import com.ssairen.backend.domain.ai.use.dto.AiTriggerResponse;
import com.ssairen.backend.domain.ai.use.dto.ToolActionResponse;
import com.ssairen.backend.domain.ai.use.service.AiUseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 데모 오케스트레이션 endpoint다. FE 트리거 수신 및 AI 종합 결과 콜백 수신을 담당한다.
 */
@RestController
@RequestMapping("/ai/use")
@Tag(name = "AI 데모 오케스트레이션", description = "FE 트리거 → FastAPI 데모 호출 및 AI 종합 결과 콜백 수신 API")
public class AiUseController {

    private final AiUseService aiUseService;

    public AiUseController(AiUseService aiUseService) {
        this.aiUseService = aiUseService;
    }

    @PostMapping("/trigger")
    @Operation(summary = "AI 데모 시작", description = "FE가 호출하면 Spring이 FastAPI 데모 파이프라인을 트리거하고 call_id 와 status 를 반환한다.")
    public AiTriggerResponse trigger(@RequestBody AiTriggerRequest request) {
        return aiUseService.triggerDemo(request);
    }

    @PostMapping("/callback")
    @Operation(summary = "AI 종합 결과 콜백 수신", description = "FastAPI가 데모 작업 완료 후 종합 결과를 push 한다. 현재는 수신만 한다(no-op).")
    public ToolActionResponse callback(@RequestBody AiAnalysisCallbackRequest request) {
        aiUseService.handleCallback(request);
        return new ToolActionResponse("callback", "RECEIVED", null);
    }
}
