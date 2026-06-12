package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.notification.dto.GuardianAlertCommand;

public interface FcmPushGateway {

    void sendGuardianAlert(GuardianAlertCommand command);
}
