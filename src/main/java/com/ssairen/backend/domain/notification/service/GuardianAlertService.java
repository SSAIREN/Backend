package com.ssairen.backend.domain.notification.service;

import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.notification.dto.GuardianAlertCommand;
import com.ssairen.backend.domain.pairing.entity.Pairing;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GuardianAlertService {

    private final PairingRepository pairingRepository;
    private final FcmPushGateway fcmPushGateway;
    private final int triggerRiskScore;

    public GuardianAlertService(
            PairingRepository pairingRepository,
            FcmPushGateway fcmPushGateway,
            @Value("${ssairen.notification.guardian-trigger-risk-score:70}") int triggerRiskScore
    ) {
        this.pairingRepository = pairingRepository;
        this.fcmPushGateway = fcmPushGateway;
        this.triggerRiskScore = triggerRiskScore;
    }

    public void sendGuardianAlertsIfNeeded(CallSession session, TranscriptAnalysisResult analysisResult) {
        /*
         * 보호자 알림은 한 통화 세션에서 한 번만 보내는 MVP 규칙을 적용한다.
         * 그래야 위험 청크가 여러 번 들어와도 가족에게 중복 푸시가 과도하게 가지 않는다.
         */
        if (analysisResult.riskScore() < triggerRiskScore) {
            return;
        }
        if (!session.markGuardianAlertSentIfNeeded()) {
            return;
        }

        List<Pairing> pairings = pairingRepository.findAllByVictimId(session.getVictim().getId());
        for (Pairing pairing : pairings) {
            User guardian = pairing.getGuardian();
            if (guardian.getFcmToken() == null || guardian.getFcmToken().isBlank()) {
                continue;
            }

            fcmPushGateway.sendGuardianAlert(new GuardianAlertCommand(
                    guardian.getId(),
                    guardian.getName(),
                    guardian.getFcmToken(),
                    session.getVictim().getId(),
                    session.getVictim().getName(),
                    session.getId(),
                    analysisResult.riskScore(),
                    analysisResult.phishingType(),
                    analysisResult.aiSummary(),
                    analysisResult.keywords()
            ));
        }
    }
}
