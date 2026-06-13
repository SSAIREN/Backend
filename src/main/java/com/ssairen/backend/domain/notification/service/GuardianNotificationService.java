package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationCommand;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationRequest;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationResponse;
import com.ssairen.backend.domain.notification.dto.GuardianSendResult;
import com.ssairen.backend.domain.pairing.entity.Pairing;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardianNotificationService {

    private final FraudCaseRepository fraudCaseRepository;
    private final PairingRepository pairingRepository;
    private final FcmPushGateway fcmPushGateway;

    public GuardianNotificationService(
            FraudCaseRepository fraudCaseRepository,
            PairingRepository pairingRepository,
            FcmPushGateway fcmPushGateway
    ) {
        this.fraudCaseRepository = fraudCaseRepository;
        this.pairingRepository = pairingRepository;
        this.fcmPushGateway = fcmPushGateway;
    }

    @Transactional(readOnly = true)
    public GuardianNotificationResponse sendGuardianNotification(GuardianNotificationRequest request) {
        fraudCaseRepository.findById(request.caseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CASE_NOT_FOUND, "케이스를 찾을 수 없습니다."));

        List<Pairing> pairings = pairingRepository.findAllByVictimId(request.victimId());

        List<GuardianSendResult> results = new ArrayList<>();
        int failCount = 0;
        for (Pairing pairing : pairings) {
            User guardian = pairing.getGuardian();

            boolean success;
            if (guardian.getFcmToken() == null || guardian.getFcmToken().isBlank()) {
                success = false;
            } else {
                success = fcmPushGateway.sendGuardianNotification(new GuardianNotificationCommand(
                        guardian.getId(),
                        guardian.getFcmToken(),
                        request.caseId(),
                        request.victimId(),
                        request.phishingType(),
                        request.message()
                ));
            }

            if (!success) {
                failCount++;
            }
            results.add(new GuardianSendResult(guardian.getId(), success));
        }

        return new GuardianNotificationResponse(results, failCount);
    }
}