package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.notification.dto.GuardianNotificationCommand;

public interface FcmPushGateway {

    boolean sendGuardianNotification(GuardianNotificationCommand command);
}
