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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/call-sessions")
@Tag(name = "?? ??", description = "Flutter ??? ?? ?? ???? ?? API")
public class CallSessionController {

    private final CallSessionService callSessionService;

    public CallSessionController(CallSessionService callSessionService) {
        this.callSessionService = callSessionService;
    }

    @PostMapping
    @Operation(
            summary = "?? ?? ??",
            description = "Flutter ?? ?? ???? ?? ? ?????. ?? externalCallId? ????? ?? ??? ?????."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "?? ?? ?? ?? ?? ?? ??"),
            @ApiResponse(
                    responseCode = "400",
                    description = "?? ? ?? ??",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CallSessionResponse> createSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Flutter? ??? ?? ?? ID? ?? ?? ??",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateCallSessionRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "externalCallId": "device-call-001",
                                      "startedAt": "2026-06-10T15:20:00+09:00",
                                      "counterpartPhoneNumber": "01012345678"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody CreateCallSessionRequest request,
            @Parameter(description = "Flutter ?? ?? ???", example = "victim-device-001")
            @RequestHeader(value = "X-Device-Id", defaultValue = "demo-device") String deviceId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(callSessionService.createSession(request, deviceId));
    }

    @GetMapping("/{sessionId}")
    @Operation(
            summary = "?? ?? ?? ??",
            description = "Flutter ?? ?????? WebSocket? ???? ? ??? ?? sequence? ?? ??? ?????."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "?? ?? ?? ?? ??"),
            @ApiResponse(
                    responseCode = "404",
                    description = "?? ??? ?? ? ??",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public CallSessionResponse getSession(
            @Parameter(description = "?? ?? ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String sessionId
    ) {
        return callSessionService.getSession(sessionId);
    }
}
