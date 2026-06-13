package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.notification.dto.GuardianAlertCommand;
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
    public void sendGuardianAlert(GuardianAlertCommand command) {
        /*
         * MVP 단계에서는 실제 Firebase Admin SDK 대신 로그 기반 더미 발송기로 동작시킨다.
         * 추후 실제 FCM 연동 시 이 구현체만 교체하면 나머지 도메인 로직은 그대로 유지할 수 있다.
         */
        log.warn(
                "[MVP-FCM] guardianUserId={}, guardianName={}, victimUserId={}, victimName={}, sessionId={}, riskScore={}, phishingType={}, token={}, summary={}",
                command.guardianUserId(),
                command.guardianName(),
                command.victimUserId(),
                command.victimName(),
                command.sessionId(),
                command.riskScore(),
                command.phishingType(),
                command.guardianFcmToken(),
                command.aiSummary()
        );
    }

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
