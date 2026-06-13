package com.ssairen.backend.domain.guardianreply.controller;

import com.ssairen.backend.domain.guardianreply.dto.GuardianReplyRequest;
import com.ssairen.backend.domain.guardianreply.dto.GuardianReplyResponse;
import com.ssairen.backend.domain.guardianreply.service.GuardianReplyService;
import com.ssairen.backend.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/guardians")
@Tag(name = "보호자 응답", description = "보호자가 보낸 안전 응답과 위치를 서버에 저장하고 피해자 앱으로 전달한다.")
public class GuardianReplyController {

    private final GuardianReplyService guardianReplyService;

    public GuardianReplyController(GuardianReplyService guardianReplyService) {
        this.guardianReplyService = guardianReplyService;
    }

    @PostMapping("/responses")
    @Operation(
            summary = "보호자 응답 접수",
            description = "보호자의 현재 위치와 메시지를 가장 최근 진행 중인 피해자 통화 세션에 연결하고, 연결된 피해자 WebSocket으로 즉시 전달한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보호자 응답 접수 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청이거나 현재 연결 가능한 피해자 통화 세션이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "보호자 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public GuardianReplyResponse createGuardianReply(@Valid @RequestBody GuardianReplyRequest request) {
        return guardianReplyService.createReply(request);
    }
}
