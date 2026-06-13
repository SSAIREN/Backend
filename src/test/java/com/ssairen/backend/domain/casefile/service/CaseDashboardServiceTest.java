package com.ssairen.backend.domain.casefile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssairen.backend.domain.casefile.dto.CaseDetailResponse;
import com.ssairen.backend.domain.casefile.dto.CaseStatusUpdateResponse;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryMetricsResponse;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryResponse;
import com.ssairen.backend.domain.casefile.entity.CaseStatus;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.responseaction.entity.ResponseAction;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionType;
import com.ssairen.backend.domain.responseaction.repository.ResponseActionRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaseDashboardServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private ResponseActionRepository responseActionRepository;

    @InjectMocks
    private CaseDashboardService caseDashboardService;

    @Test
    void status가_없으면_전체_케이스를_탐지시각_역순으로_조회한다() {
        FraudCase fraudCase = fraudCase(CaseStatus.IN_PROGRESS);
        given(fraudCaseRepository.findAllByOrderByDetectedAtDesc()).willReturn(List.of(fraudCase));

        List<CaseSummaryResponse> result = caseDashboardService.getCases(null);

        assertThat(result).hasSize(1);
        verify(fraudCaseRepository, never()).findByStatusOrderByDetectedAtDesc(any());
    }

    @Test
    void status가_있으면_해당_상태의_케이스만_조회한다() {
        FraudCase fraudCase = fraudCase(CaseStatus.COMPLETED);
        given(fraudCaseRepository.findByStatusOrderByDetectedAtDesc(CaseStatus.COMPLETED))
                .willReturn(List.of(fraudCase));

        List<CaseSummaryResponse> result = caseDashboardService.getCases(CaseStatus.COMPLETED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(CaseStatus.COMPLETED);
        verify(fraudCaseRepository, never()).findAllByOrderByDetectedAtDesc();
    }

    @Test
    void 케이스를_대시보드_응답_DTO로_변환한다() {
        FraudCase fraudCase = fraudCase(CaseStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(fraudCase, "id", 1L);
        ReflectionTestUtils.setField(fraudCase, "riskScore", 85);
        ReflectionTestUtils.setField(fraudCase, "phishingType", PhishingType.KIDNAPPING_THREAT);
        ReflectionTestUtils.setField(fraudCase, "region", "서울특별시 강남구");
        ReflectionTestUtils.setField(fraudCase, "callDurationSec", 180);
        given(fraudCaseRepository.findAllByOrderByDetectedAtDesc()).willReturn(List.of(fraudCase));

        CaseSummaryResponse response = caseDashboardService.getCases(null).get(0);

        assertThat(response.caseId()).isEqualTo(1L);
        assertThat(response.victimName()).isEqualTo("김OO");
        assertThat(response.age()).isEqualTo(71);
        assertThat(response.riskScore()).isEqualTo(85);
        assertThat(response.phishingType()).isEqualTo(PhishingType.KIDNAPPING_THREAT);
        assertThat(response.region()).isEqualTo("서울특별시 강남구");
        assertThat(response.callDurationSec()).isEqualTo(180);
    }

    @Test
    void 케이스_상세_조회시_AI_요약과_위치와_대응조치_목록을_반환한다() {
        FraudCase fraudCase = fraudCase(CaseStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(fraudCase, "id", 1L);
        ReflectionTestUtils.setField(fraudCase, "aiSummary", "검찰 수사관을 사칭하여 계좌 이체를 유도함");
        ReflectionTestUtils.setField(fraudCase, "keywords", "검찰, 계좌 이체, 안전 계좌");
        ReflectionTestUtils.setField(fraudCase, "latitude", new BigDecimal("37.4979"));
        ReflectionTestUtils.setField(fraudCase, "longitude", new BigDecimal("127.0276"));
        given(fraudCaseRepository.findById(1L)).willReturn(Optional.of(fraudCase));

        ResponseAction action = new ResponseAction(fraudCase, ResponseActionType.GPS);
        given(responseActionRepository.findByFraudCaseIdOrderByExecutedAtAsc(1L)).willReturn(List.of(action));

        CaseDetailResponse response = caseDashboardService.getCaseDetail(1L);

        assertThat(response.caseId()).isEqualTo(1L);
        assertThat(response.aiSummary()).isEqualTo("검찰 수사관을 사칭하여 계좌 이체를 유도함");
        assertThat(response.keywords()).isEqualTo("검찰, 계좌 이체, 안전 계좌");
        assertThat(response.latitude()).isEqualTo(new BigDecimal("37.4979"));
        assertThat(response.longitude()).isEqualTo(new BigDecimal("127.0276"));
        assertThat(response.actions()).hasSize(1);
        assertThat(response.actions().get(0).actionType()).isEqualTo(ResponseActionType.GPS);
    }

    @Test
    void 존재하지_않는_케이스를_조회하면_예외가_발생한다() {
        given(fraudCaseRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseDashboardService.getCaseDetail(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CASE_NOT_FOUND);
    }

    @Test
    void 요약_지표는_진행중_완료_건수와_평균_대응시간을_계산한다() {
        given(fraudCaseRepository.countByStatus(CaseStatus.IN_PROGRESS)).willReturn(3L);

        OffsetDateTime now = OffsetDateTime.now();
        FraudCase fast = fraudCase(CaseStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(fast, "detectedAt", now);
        ReflectionTestUtils.setField(fast, "respondedAt", now.plusSeconds(100));
        FraudCase slow = fraudCase(CaseStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(slow, "detectedAt", now);
        ReflectionTestUtils.setField(slow, "respondedAt", now.plusSeconds(200));
        given(fraudCaseRepository.findByStatusOrderByDetectedAtDesc(CaseStatus.COMPLETED))
                .willReturn(List.of(fast, slow));

        CaseSummaryMetricsResponse response = caseDashboardService.getSummaryMetrics();

        assertThat(response.inProgress()).isEqualTo(3L);
        assertThat(response.completed()).isEqualTo(2L);
        assertThat(response.avgResponseSec()).isEqualTo(150L);
    }

    @Test
    void 완료된_케이스가_없으면_평균_대응시간은_null이다() {
        given(fraudCaseRepository.countByStatus(CaseStatus.IN_PROGRESS)).willReturn(0L);
        given(fraudCaseRepository.findByStatusOrderByDetectedAtDesc(CaseStatus.COMPLETED)).willReturn(List.of());

        CaseSummaryMetricsResponse response = caseDashboardService.getSummaryMetrics();

        assertThat(response.inProgress()).isEqualTo(0L);
        assertThat(response.completed()).isEqualTo(0L);
        assertThat(response.avgResponseSec()).isNull();
    }

    @Test
    void 케이스_상태를_완료로_변경하면_응답시각이_채워진다() {
        FraudCase fraudCase = fraudCase(CaseStatus.IN_PROGRESS);
        given(fraudCaseRepository.findById(1L)).willReturn(Optional.of(fraudCase));

        CaseStatusUpdateResponse response = caseDashboardService.updateStatus(1L, CaseStatus.COMPLETED);

        assertThat(response.success()).isTrue();
        assertThat(fraudCase.getStatus()).isEqualTo(CaseStatus.COMPLETED);
        assertThat(fraudCase.getRespondedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_케이스의_상태를_변경하면_예외가_발생한다() {
        given(fraudCaseRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> caseDashboardService.updateStatus(999L, CaseStatus.COMPLETED))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CASE_NOT_FOUND);
    }

    private FraudCase fraudCase(CaseStatus status) {
        User victim = new User("김OO", UserRole.VICTIM, 71, "01012345678");
        FraudCase fraudCase = new FraudCase(victim, OffsetDateTime.now());
        if (status == CaseStatus.COMPLETED) {
            fraudCase.complete(OffsetDateTime.now().minusMinutes(5), OffsetDateTime.now());
        }
        return fraudCase;
    }
}