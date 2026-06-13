package com.ssairen.backend.domain.stats.controller;

import com.ssairen.backend.domain.stats.dto.AgeGroupStatResponse;
import com.ssairen.backend.domain.stats.dto.FrozenAccountStatResponse;
import com.ssairen.backend.domain.stats.dto.KeywordStatResponse;
import com.ssairen.backend.domain.stats.dto.PhoneNumberStatResponse;
import com.ssairen.backend.domain.stats.dto.StatsOverviewResponse;
import com.ssairen.backend.domain.stats.service.DashboardStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@Tag(
        name = "경찰 관제 대시보드 통계",
        description = "관제 대시보드 상단 카드, 키워드 통계, 연령대 통계, 위험 발신번호, 지급정지 계좌 현황을 조회하는 API"
)
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    public DashboardStatsController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/overview")
    @Operation(
            summary = "대시보드 개요 통계 조회",
            description = "전체 탐지 건수, 고위험 탐지 건수, 지급정지 계좌 수처럼 대시보드 상단 카드에 노출할 핵심 요약 지표를 조회합니다."
    )
    public StatsOverviewResponse getOverview() {
        return dashboardStatsService.getOverview();
    }

    @GetMapping("/keywords")
    @Operation(
            summary = "탐지 키워드 상위 통계 조회",
            description = "보이스피싱 분석 과정에서 가장 자주 탐지된 키워드 상위 목록을 조회합니다. 대시보드의 키워드 빈도 차트나 랭킹 영역에 사용할 수 있습니다."
    )
    public List<KeywordStatResponse> getKeywordStats() {
        return dashboardStatsService.getKeywordStats();
    }

    @GetMapping("/age-groups")
    @Operation(
            summary = "피해자 연령대 통계 조회",
            description = "피해자 연령대별 탐지 건수와 비율을 조회합니다. 연령 분포 차트나 비중 분석 영역에 사용할 수 있습니다."
    )
    public List<AgeGroupStatResponse> getAgeGroupStats() {
        return dashboardStatsService.getAgeGroupStats();
    }

    @GetMapping("/phone-numbers")
    @Operation(
            summary = "위험 발신번호 통계 조회",
            description = "탐지 횟수가 많은 위험 발신번호 목록을 조회합니다. 반복적으로 신고되거나 탐지된 번호를 우선 모니터링하는 화면에 사용할 수 있습니다."
    )
    public List<PhoneNumberStatResponse> getPhoneNumberStats() {
        return dashboardStatsService.getPhoneNumberStats();
    }

    @GetMapping("/frozen-accounts")
    @Operation(
            summary = "지급정지 계좌 현황 조회",
            description = "지급정지 요청이 등록된 계좌 목록과 처리 상태를 조회합니다. 금융기관 대응 현황이나 후속 조치 관리 화면에 사용할 수 있습니다."
    )
    public List<FrozenAccountStatResponse> getFrozenAccountStats() {
        return dashboardStatsService.getFrozenAccountStats();
    }
}
