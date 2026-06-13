package com.ssairen.backend.domain.notification.controller;

import com.ssairen.backend.domain.notification.dto.GuardianNotificationRequest;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationResponse;
import com.ssairen.backend.domain.notification.service.GuardianNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@Tag(
        name = "알림",
        description = "보호자에게 보이스피싱 위험 알림을 FCM 푸시로 발송하는 API"
)
public class NotificationController {

    private final GuardianNotificationService guardianNotificationService;

    public NotificationController(GuardianNotificationService guardianNotificationService) {
        this.guardianNotificationService = guardianNotificationService;
    }

    @PostMapping("/guardian")
    @Operation(
            summary = "보호자 FCM 푸시 발송",
            description = "피해자와 페어링된 보호자들에게 보이스피싱 위험 알림을 FCM 푸시로 발송합니다."
    )
    public GuardianNotificationResponse sendGuardianNotification(
            @Valid @RequestBody GuardianNotificationRequest request
    ) {
        return guardianNotificationService.sendGuardianNotification(request);
    }
}
