package com.ssairen.backend.domain.ai.use.controller;

import com.ssairen.backend.domain.ai.use.dto.EvidenceRequest;
import com.ssairen.backend.domain.ai.use.dto.FamilyAlertRequest;
import com.ssairen.backend.domain.ai.use.dto.GpsRequest;
import com.ssairen.backend.domain.ai.use.dto.PoliceReportRequest;
import com.ssairen.backend.domain.ai.use.dto.ToolActionResponse;
import com.ssairen.backend.domain.ai.use.dto.WarningBannerRequest;
import com.ssairen.backend.domain.ai.use.service.AiToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI(FastAPI) tool 콜백 5종을 수신하는 endpoint다. 전부 POST이며 데모 단계에서는 수신·로그·성공응답만 수행한다.
 */
@RestController
@RequestMapping("/ai/use")
@Tag(name = "AI tool 콜백", description = "FastAPI tool 콜링(FCM/증거/경찰/GPS) 수신 API")
public class AiToolCallbackController {

    private final AiToolService aiToolService;

    public AiToolCallbackController(AiToolService aiToolService) {
        this.aiToolService = aiToolService;
    }

    @PostMapping("/notifications/family-alert")
    @Operation(summary = "가족 긴급 알림 (send_family_sms_alert)")
    public ToolActionResponse familyAlert(@RequestBody FamilyAlertRequest request) {
        return aiToolService.familyAlert(request);
    }

    @PostMapping("/notifications/warning-banner")
    @Operation(summary = "경고 배너 표시 (show_warning_banner)")
    public ToolActionResponse warningBanner(@RequestBody WarningBannerRequest request) {
        return aiToolService.warningBanner(request);
    }

    @PostMapping("/evidence")
    @Operation(summary = "증거 저장 (save_evidence)")
    public ToolActionResponse saveEvidence(@RequestBody EvidenceRequest request) {
        return aiToolService.saveEvidence(request);
    }

    @PostMapping("/police/report")
    @Operation(summary = "경찰 신고 (notify_police)")
    public ToolActionResponse notifyPolice(@RequestBody PoliceReportRequest request) {
        return aiToolService.notifyPolice(request);
    }

    @PostMapping("/family/gps")
    @Operation(summary = "가족 GPS 확인 (check_family_gps)")
    public ToolActionResponse familyGps(@RequestBody GpsRequest request) {
        return aiToolService.familyGps(request);
    }
}
