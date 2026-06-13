package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.notification.dto.GuardianNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ssairen.fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingFcmPushGateway implements FcmPushGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingFcmPushGateway.class);

    @Override
    public boolean sendGuardianNotification(GuardianNotificationCommand command) {
        log.warn(
                "[MVP-FCM] guardianUserId={}, caseId={}, victimUserId={}, phishingType={}, token={}, message={}",
                command.guardianUserId(),
                command.caseId(),
                command.victimId(),
                command.phishingType(),
                command.guardianFcmToken(),
                command.message()
        );
        return true;
    }
}
