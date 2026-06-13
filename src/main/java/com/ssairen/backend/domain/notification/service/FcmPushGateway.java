package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.notification.dto.GuardianAlertCommand;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationCommand;

public interface FcmPushGateway {

    void sendGuardianAlert(GuardianAlertCommand command);

    boolean sendGuardianNotification(GuardianNotificationCommand command);
}
