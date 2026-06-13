package com.ssairen.backend.domain.user.controller;

import com.ssairen.backend.domain.user.dto.FcmTokenUpdateRequest;
import com.ssairen.backend.domain.user.dto.FcmTokenUpdateResponse;
import com.ssairen.backend.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(
        name = "사용자",
        description = "FCM 토큰 등록 등 사용자 정보 관리 API"
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/{userId}/fcm-token")
    @Operation(
            summary = "FCM 토큰 등록·갱신",
            description = "사용자 디바이스의 FCM 토큰을 등록하거나 갱신합니다."
    )
    public FcmTokenUpdateResponse updateFcmToken(
            @Parameter(description = "FCM 토큰을 등록할 사용자 ID", example = "1001")
            @PathVariable Long userId,
            @Valid @RequestBody FcmTokenUpdateRequest request
    ) {
        return userService.updateFcmToken(userId, request.fcmToken());
    }
}