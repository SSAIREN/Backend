package com.ssairen.backend.domain.callsession.controller;

import com.ssairen.backend.domain.callsession.dto.CallSessionResponse;
import com.ssairen.backend.domain.callsession.dto.CreateCallSessionRequest;
import com.ssairen.backend.domain.callsession.service.CallSessionService;
import com.ssairen.backend.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/call-sessions")
@Tag(name = "통화 세션", description = "Flutter 피해자 앱의 통화 모니터링 세션 API")
public class CallSessionController {

    private final CallSessionService callSessionService;

    public CallSessionController(CallSessionService callSessionService) {
        this.callSessionService = callSessionService;
    }

    @PostMapping
    @Operation(
            summary = "통화 세션 생성",
            description = "Flutter 앱이 통화 모니터링 시작 시 호출합니다. 같은 externalCallId를 재전송하면 기존 세션을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "통화 세션 생성 또는 기존 세션 반환"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CallSessionResponse> createSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Flutter가 생성한 외부 통화 ID와 통화 시작 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateCallSessionRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "externalCallId": "device-call-001",
                                      "deviceId": "victim-device-001",
                                      "startedAt": "2026-06-10T15:20:00+09:00",
                                      "phoneNumber": "01012345678",
                                      "victim": {
                                        "name": "김OO",
                                        "age": 71
                                      }
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody CreateCallSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(callSessionService.createSession(request));
    }

    @GetMapping("/{sessionId}")
    @Operation(
            summary = "통화 세션 상태 조회",
            description = "Flutter 앱이 재실행되거나 WebSocket을 재연결할 때 서버의 현재 sequence와 세션 상태를 복구합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "통화 세션 상태 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "통화 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public CallSessionResponse getSession(
            @Parameter(description = "통화 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId
    ) {
        return callSessionService.getSession(sessionId);
    }
}
