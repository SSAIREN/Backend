package com.ssairen.backend.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ssairen.fcm", name = "enabled", havingValue = "true")
public class FirebaseFcmPushGateway implements FcmPushGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebaseFcmPushGateway.class);

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseFcmPushGateway(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public boolean sendGuardianNotification(GuardianNotificationCommand command) {
        Message message = Message.builder()
                .setToken(command.guardianFcmToken())
                .setNotification(Notification.builder()
                        .setTitle("[SSAIREN] 보이스피싱 위험 알림")
                        .setBody(command.message())
                        .build())
                .putData("caseId", String.valueOf(command.caseId()))
                .putData("victimUserId", String.valueOf(command.victimId()))
                .putData("phishingType", command.phishingType().name())
                .build();

        try {
            firebaseMessaging.send(message);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error(
                    "FCM 푸시 발송 실패: guardianUserId={}, caseId={}, errorCode={}",
                    command.guardianUserId(),
                    command.caseId(),
                    e.getMessagingErrorCode(),
                    e
            );
            return false;
        }
    }
}