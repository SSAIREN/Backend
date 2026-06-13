package com.ssairen.backend.domain.ai.use.service;

import com.ssairen.backend.domain.ai.use.dto.EvidenceRequest;
import com.ssairen.backend.domain.ai.use.dto.FamilyAlertRequest;
import com.ssairen.backend.domain.ai.use.dto.GpsRequest;
import com.ssairen.backend.domain.ai.use.dto.PoliceReportRequest;
import com.ssairen.backend.domain.ai.use.dto.ToolActionResponse;
import com.ssairen.backend.domain.ai.use.dto.WarningBannerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI(FastAPI) tool 콜백을 처리하는 서비스다.
 * 데모 단계에서는 요청 수신·로그·성공응답만 수행한다.
 */
@Service
public class AiToolService {

    private static final Logger log = LoggerFactory.getLogger(AiToolService.class);

    public ToolActionResponse familyAlert(FamilyAlertRequest req) {
        log.info("tool[send_family_sms_alert] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 실제 FCM/GPS/증거저장/경찰신고 연동 지점
        return ToolActionResponse.ok("send_family_sms_alert");
    }

    public ToolActionResponse warningBanner(WarningBannerRequest req) {
        log.info("tool[show_warning_banner] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 실제 FCM/GPS/증거저장/경찰신고 연동 지점
        return ToolActionResponse.ok("show_warning_banner");
    }

    public ToolActionResponse saveEvidence(EvidenceRequest req) {
        log.info("tool[save_evidence] 수신: sessionId={}, scenario={}, riskScore={}",
                req.sessionId(), req.scenario(), req.riskScore());
        // TODO: 실제 FCM/GPS/증거저장/경찰신고 연동 지점
        return ToolActionResponse.ok("save_evidence");
    }

    public ToolActionResponse notifyPolice(PoliceReportRequest req) {
        log.info("tool[notify_police] 수신: sessionId={}, incidentType={}, riskScore={}",
                req.sessionId(), req.incidentType(), req.riskScore());
        // TODO: 실제 FCM/GPS/증거저장/경찰신고 연동 지점
        return ToolActionResponse.ok("notify_police");
    }

    public ToolActionResponse familyGps(GpsRequest req) {
        log.info("tool[check_family_gps] 수신: sessionId={}", req.sessionId());
        // TODO: 실제 FCM/GPS/증거저장/경찰신고 연동 지점
        return ToolActionResponse.ok("check_family_gps");
    }
}
