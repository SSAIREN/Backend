package com.ssairen.backend.domain.ai.use.service;

import com.ssairen.backend.domain.ai.use.dto.EvidenceRequest;
import com.ssairen.backend.domain.ai.use.dto.FamilyAlertRequest;
import com.ssairen.backend.domain.ai.use.dto.GpsRequest;
import com.ssairen.backend.domain.ai.use.dto.PoliceReportRequest;
import com.ssairen.backend.domain.ai.use.dto.ToolActionResponse;
import com.ssairen.backend.domain.ai.use.dto.WarningBannerRequest;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationRequest;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationResponse;
import com.ssairen.backend.domain.notification.service.GuardianNotificationService;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI(FastAPI) tool 콜백을 처리하는 서비스다.
 * check_family_gps 는 GuardianNotificationService(보호자 FCM)를 재사용해 실제 발송한다.
 * 나머지 tool 은 데모 단계에서 요청 수신·로그·성공응답만 수행한다.
 */
@Service
public class AiToolService {

    private static final Logger log = LoggerFactory.getLogger(AiToolService.class);

    private static final String GPS_GUARDIAN_MESSAGE =
            "[SSAIREN] 가족이 납치 협박 의심 통화를 받고 있습니다. 안전 확인을 위해 현재 위치 공유가 필요합니다.";

    private final CallSessionRepository callSessionRepository;
    private final GuardianNotificationService guardianNotificationService;

    public AiToolService(
            CallSessionRepository callSessionRepository,
            GuardianNotificationService guardianNotificationService
    ) {
        this.callSessionRepository = callSessionRepository;
        this.guardianNotificationService = guardianNotificationService;
    }

    public ToolActionResponse familyAlert(FamilyAlertRequest req) {
        log.info("tool[send_family_sms_alert] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 후속 — family-alert 실제 연동(보류). 필요 시 GuardianNotificationService 재사용.
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

    public ToolActionResponse notifyPolice(PoliceReportRequest req) {
        log.info("tool[notify_police] 수신: sessionId={}, incidentType={}, riskScore={}",
                req.sessionId(), req.incidentType(), req.riskScore());
        // TODO: 후속 — response_actions 기록/대시보드 통지(대시보드 팀 영역).
        return ToolActionResponse.ok("notify_police");
    }

    /**
     * check_family_gps: sessionId 로 피해자/케이스를 찾아, 페어링된 보호자에게
     * 위치 확인 요청 FCM 을 발송한다. GuardianNotificationService(/notifications/guardian) 재사용.
     */
    @Transactional(readOnly = true)
    public ToolActionResponse familyGps(GpsRequest req) {
        CallSession session = callSessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CALL_SESSION_NOT_FOUND, "통화 세션을 찾을 수 없습니다."));

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
}
