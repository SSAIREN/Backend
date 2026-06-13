package com.ssairen.backend.domain.ai.use.service;

import com.ssairen.backend.domain.ai.use.dto.EvidenceRequest;
import com.ssairen.backend.domain.ai.use.dto.FamilyAlertRequest;
import com.ssairen.backend.domain.ai.use.dto.GpsRequest;
import com.ssairen.backend.domain.ai.use.dto.PoliceReportRequest;
import com.ssairen.backend.domain.ai.use.dto.ToolActionResponse;
import com.ssairen.backend.domain.ai.use.dto.WarningBannerRequest;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.domain.casefile.service.DashboardNotificationService;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationRequest;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationResponse;
import com.ssairen.backend.domain.notification.service.GuardianNotificationService;
import com.ssairen.backend.domain.responseaction.entity.ResponseAction;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionType;
import com.ssairen.backend.domain.responseaction.repository.ResponseActionRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI(FastAPI) tool 콜백을 처리하는 서비스다.
 * - check_family_gps: GuardianNotificationService(보호자 FCM)를 재사용해 발송한다.
 * - notify_police: ResponseAction(POLICE) 기록 후 경찰 대시보드로 broadcast 한다.
 * - 그 외 tool 은 데모 단계에서 요청 수신·로그·성공응답만 수행한다.
 */
@Service
public class AiToolService {

    private static final Logger log = LoggerFactory.getLogger(AiToolService.class);

    private static final String GPS_GUARDIAN_MESSAGE =
            "[SSAIREN] 가족이 납치 협박 의심 통화를 받고 있습니다. 안전 확인을 위해 현재 위치 공유가 필요합니다.";

    private final CallSessionRepository callSessionRepository;
    private final GuardianNotificationService guardianNotificationService;
    private final ResponseActionRepository responseActionRepository;
    private final DashboardNotificationService dashboardNotificationService;

    public AiToolService(
            CallSessionRepository callSessionRepository,
            GuardianNotificationService guardianNotificationService,
            ResponseActionRepository responseActionRepository,
            DashboardNotificationService dashboardNotificationService
    ) {
        this.callSessionRepository = callSessionRepository;
        this.guardianNotificationService = guardianNotificationService;
        this.responseActionRepository = responseActionRepository;
        this.dashboardNotificationService = dashboardNotificationService;
    }

    public ToolActionResponse familyAlert(FamilyAlertRequest req) {
        log.info("tool[send_family_sms_alert] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 후속 — family-alert 실제 연동(보류).
        return ToolActionResponse.ok("send_family_sms_alert");
    }

    public ToolActionResponse warningBanner(WarningBannerRequest req) {
        log.info("tool[show_warning_banner] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 후속 — 피해자 경고 배너(스킵).
        return ToolActionResponse.ok("show_warning_banner");
    }

    public ToolActionResponse saveEvidence(EvidenceRequest req) {
        log.info("tool[save_evidence] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 후속 — 증거 저장(보류).
        return ToolActionResponse.ok("save_evidence");
    }

    /**
     * notify_police: sessionId 로 케이스를 찾아 POLICE 대응 조치를 기록하고,
     * 경찰 대시보드로 ACTION_UPDATE 를 broadcast 한다. (response_actions + DashboardNotificationService 재사용)
     */
    @Transactional
    public ToolActionResponse notifyPolice(PoliceReportRequest req) {
        log.info("tool[notify_police] 수신: sessionId={}, incidentType={}, riskScore={}",
                req.sessionId(), req.incidentType(), req.riskScore());

        CallSession session = loadSession(req.sessionId());
        if (!session.markPoliceNotifiedIfNeeded()) {
            log.info("tool[notify_police] 이미 통지된 세션이라 중복 호출을 건너뜁니다: sessionId={}", req.sessionId());
            return ToolActionResponse.skipped("notify_police", "already notified");
        }

        FraudCase fraudCase = session.getFraudCase();

        ResponseAction action = new ResponseAction(fraudCase, ResponseActionType.POLICE);
        action.markCompleted(OffsetDateTime.now());
        responseActionRepository.save(action);
        dashboardNotificationService.broadcastActionUpdate(
                fraudCase.getId(), action.getActionType(), action.getStatus());

        log.info("tool[notify_police] 대시보드 기록·통지 완료: caseId={}, action=POLICE/{}",
                fraudCase.getId(), action.getStatus());
        return ToolActionResponse.ok("notify_police", "caseId=" + fraudCase.getId() + ", action=POLICE");
    }

    /**
     * check_family_gps: sessionId 로 피해자/케이스를 찾아, 페어링된 보호자에게
     * 위치 확인 요청 FCM 을 발송한다. GuardianNotificationService(/notifications/guardian) 재사용.
     */
    @Transactional
    public ToolActionResponse familyGps(GpsRequest req) {
        log.info("tool[check_family_gps] 수신: sessionId={}", req.sessionId());

        CallSession session = loadSession(req.sessionId());
        if (!session.markFamilyGpsNotifiedIfNeeded()) {
            log.info("tool[check_family_gps] 이미 발송된 세션이라 중복 호출을 건너뜁니다: sessionId={}", req.sessionId());
            return ToolActionResponse.skipped("check_family_gps", "already notified");
        }
        Long victimId = session.getVictim().getId();
        Long caseId = session.getFraudCase().getId();

        GuardianNotificationResponse result = guardianNotificationService.sendGuardianNotification(
                new GuardianNotificationRequest(
                        caseId,
                        victimId,
                        PhishingType.KIDNAPPING_THREAT,
                        GPS_GUARDIAN_MESSAGE
                )
        );

        log.info("tool[check_family_gps] 발송 완료: sessionId={}, caseId={}, victimId={}, sent={}, fail={}",
                req.sessionId(), caseId, victimId, result.sent().size(), result.failCount());

        return ToolActionResponse.ok(
                "check_family_gps",
                "guardians=" + result.sent().size() + ", fail=" + result.failCount());
    }

    private CallSession loadSession(String sessionId) {
        return callSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));
    }
}
