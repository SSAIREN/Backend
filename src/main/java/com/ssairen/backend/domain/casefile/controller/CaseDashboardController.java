package com.ssairen.backend.domain.casefile.controller;

import com.ssairen.backend.domain.casefile.dto.CaseDetailResponse;
import com.ssairen.backend.domain.casefile.dto.CaseStatusUpdateRequest;
import com.ssairen.backend.domain.casefile.dto.CaseStatusUpdateResponse;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryMetricsResponse;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryResponse;
import com.ssairen.backend.domain.casefile.entity.CaseStatus;
import com.ssairen.backend.domain.casefile.service.CaseDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cases")
@Tag(
        name = "경찰 관제 대시보드 케이스",
        description = "보이스피싱 탐지 케이스 목록, 요약 지표, 상세 조회, 상태 변경을 제공하는 대시보드 API"
)
public class CaseDashboardController {

    private final CaseDashboardService caseDashboardService;

    public CaseDashboardController(CaseDashboardService caseDashboardService) {
        this.caseDashboardService = caseDashboardService;
    }

    @GetMapping
    @Operation(
            summary = "케이스 목록 조회",
            description = "관제 대시보드에 표시할 케이스 목록을 조회합니다. 필요하면 진행 상태로 필터링할 수 있습니다."
    )
    public List<CaseSummaryResponse> getCases(
            @Parameter(
                    description = "케이스 진행 상태 필터. 생략하면 전체 상태를 조회합니다.",
                    example = "IN_PROGRESS"
            )
            @RequestParam(required = false) CaseStatus status
    ) {
        return caseDashboardService.getCases(status);
    }

    @GetMapping("/summary")
    @Operation(
            summary = "케이스 요약 지표 조회",
            description = "진행중 케이스 수, 완료된 케이스 수, 평균 대응 시간처럼 대시보드 상단 요약 영역에 사용할 핵심 지표를 조회합니다."
    )
    public CaseSummaryMetricsResponse getSummary() {
        return caseDashboardService.getSummaryMetrics();
    }

    @GetMapping("/{caseId}")
    @Operation(
            summary = "케이스 상세 조회",
            description = "단일 케이스의 AI 분석 요약, 주요 키워드, 위치 정보, 대응 조치 이력 등 상세 정보를 조회합니다."
    )
    public CaseDetailResponse getCaseDetail(
            @Parameter(description = "조회할 케이스 ID", example = "1")
            @PathVariable Long caseId
    ) {
        return caseDashboardService.getCaseDetail(caseId);
    }

    @PatchMapping("/{caseId}/status")
    @Operation(
            summary = "케이스 상태 변경",
            description = "관제 담당자가 케이스 상태를 변경할 때 사용하는 API입니다. 예를 들어 진행중 케이스를 완료 상태로 전환할 수 있습니다."
    )
    public CaseStatusUpdateResponse updateStatus(
            @Parameter(description = "상태를 변경할 케이스 ID", example = "1")
            @PathVariable Long caseId,
            @Valid @RequestBody CaseStatusUpdateRequest request
    ) {
        return caseDashboardService.updateStatus(caseId, request.status());
    }
}
