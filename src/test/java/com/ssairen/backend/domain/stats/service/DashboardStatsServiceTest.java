package com.ssairen.backend.domain.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.frozenaccount.entity.FrozenAccount;
import com.ssairen.backend.domain.frozenaccount.repository.FrozenAccountRepository;
import com.ssairen.backend.domain.phone.entity.BlockStatus;
import com.ssairen.backend.domain.phone.entity.PhoneNumberRecord;
import com.ssairen.backend.domain.phone.repository.PhoneNumberRecordRepository;
import com.ssairen.backend.domain.stats.dto.AgeGroupStatResponse;
import com.ssairen.backend.domain.stats.dto.KeywordStatResponse;
import com.ssairen.backend.domain.stats.dto.PhoneNumberStatResponse;
import com.ssairen.backend.domain.stats.dto.StatsOverviewResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class   DashboardStatsServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private PhoneNumberRecordRepository phoneNumberRecordRepository;

    @Mock
    private FrozenAccountRepository frozenAccountRepository;

    @InjectMocks
    private DashboardStatsService dashboardStatsService;

    @Test
    void 통계_개요는_전체_탐지건수와_고위험건수와_지급정지계좌수를_반환한다() {
        given(fraudCaseRepository.count()).willReturn(120L);
        given(fraudCaseRepository.countByRiskScoreGreaterThanEqual(80)).willReturn(32L);
        given(frozenAccountRepository.count()).willReturn(15L);

        StatsOverviewResponse response = dashboardStatsService.getOverview();

        assertThat(response.totalDetections()).isEqualTo(120L);
        assertThat(response.highRisk()).isEqualTo(32L);
        assertThat(response.frozenAccounts()).isEqualTo(15L);
    }

    @Test
    void 키워드_통계는_빈도수_상위_10개를_순위와_함께_반환한다() {
        given(fraudCaseRepository.findAllKeywords()).willReturn(List.of(
                "검찰, 계좌 이체, 안전 계좌",
                "안전 계좌, 명의도용",
                "안전 계좌"
        ));

        List<KeywordStatResponse> result = dashboardStatsService.getKeywordStats();

        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).keyword()).isEqualTo("안전 계좌");
        assertThat(result.get(0).count()).isEqualTo(3L);
    }

    @Test
    void 연령대별_통계는_구간별_건수와_비율을_반환한다() {
        given(fraudCaseRepository.findAllVictimAges()).willReturn(List.of(25, 35, 71, 68));

        List<AgeGroupStatResponse> result = dashboardStatsService.getAgeGroupStats();

        assertThat(result).extracting(AgeGroupStatResponse::ageGroup)
                .containsExactly("20대 이하", "30대", "40대", "50대", "60대 이상");

        AgeGroupStatResponse under30 = result.get(0);
        assertThat(under30.count()).isEqualTo(1L);
        assertThat(under30.ratio()).isEqualTo(25.0);

        AgeGroupStatResponse over60 = result.get(4);
        assertThat(over60.count()).isEqualTo(2L);
        assertThat(over60.ratio()).isEqualTo(50.0);
    }

    @Test
    void 위험_발신번호_통계는_탐지횟수_내림차순으로_반환한다() {
        PhoneNumberRecord record = new PhoneNumberRecord("010-1234-5678");
        ReflectionTestUtils.setField(record, "detectionCount", 12);
        ReflectionTestUtils.setField(record, "lastDetectedAt", OffsetDateTime.now());
        ReflectionTestUtils.setField(record, "blockStatus", BlockStatus.BLOCKED);
        given(phoneNumberRecordRepository.findAllByOrderByDetectionCountDesc()).willReturn(List.of(record));

        List<PhoneNumberStatResponse> result = dashboardStatsService.getPhoneNumberStats();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.get(0).detectionCount()).isEqualTo(12);
        assertThat(result.get(0).blockStatus()).isEqualTo(BlockStatus.BLOCKED);
    }

    @Test
    void 지급정지_계좌_통계는_전체_목록을_반환한다() {
        FrozenAccount account = new FrozenAccount("국민은행", "123456-78-901234");
        given(frozenAccountRepository.findAllByOrderByRequestedAtDesc()).willReturn(List.of(account));

        var result = dashboardStatsService.getFrozenAccountStats();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bankName()).isEqualTo("국민은행");
        assertThat(result.get(0).accountNumber()).isEqualTo("123456-78-901234");
    }
}
