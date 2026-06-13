package com.ssairen.backend.domain.stats.service;

import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.frozenaccount.repository.FrozenAccountRepository;
import com.ssairen.backend.domain.phone.repository.PhoneNumberRecordRepository;
import com.ssairen.backend.domain.stats.dto.AgeGroupStatResponse;
import com.ssairen.backend.domain.stats.dto.FrozenAccountStatResponse;
import com.ssairen.backend.domain.stats.dto.KeywordStatResponse;
import com.ssairen.backend.domain.stats.dto.PhoneNumberStatResponse;
import com.ssairen.backend.domain.stats.dto.StatsOverviewResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardStatsService {

    private static final int HIGH_RISK_THRESHOLD = 80;
    private static final int KEYWORD_TOP_COUNT = 10;

    private final FraudCaseRepository fraudCaseRepository;
    private final PhoneNumberRecordRepository phoneNumberRecordRepository;
    private final FrozenAccountRepository frozenAccountRepository;

    public DashboardStatsService(
            FraudCaseRepository fraudCaseRepository,
            PhoneNumberRecordRepository phoneNumberRecordRepository,
            FrozenAccountRepository frozenAccountRepository
    ) {
        this.fraudCaseRepository = fraudCaseRepository;
        this.phoneNumberRecordRepository = phoneNumberRecordRepository;
        this.frozenAccountRepository = frozenAccountRepository;
    }

    @Transactional(readOnly = true)
    public StatsOverviewResponse getOverview() {
        long totalDetections = fraudCaseRepository.count();
        long highRisk = fraudCaseRepository.countByRiskScoreGreaterThanEqual(HIGH_RISK_THRESHOLD);
        long frozenAccounts = frozenAccountRepository.count();

        return new StatsOverviewResponse(totalDetections, highRisk, frozenAccounts);
    }

    @Transactional(readOnly = true)
    public List<KeywordStatResponse> getKeywordStats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String keywords : fraudCaseRepository.findAllKeywords()) {
            for (String keyword : keywords.split(",")) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty()) {
                    counts.merge(trimmed, 1L, Long::sum);
                }
            }
        }

        List<KeywordStatResponse> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Long> entry : counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(KEYWORD_TOP_COUNT)
                .toList()) {
            result.add(new KeywordStatResponse(rank++, entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AgeGroupStatResponse> getAgeGroupStats() {
        List<Integer> ages = fraudCaseRepository.findAllVictimAges();

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("20대 이하", 0L);
        counts.put("30대", 0L);
        counts.put("40대", 0L);
        counts.put("50대", 0L);
        counts.put("60대 이상", 0L);

        for (Integer age : ages) {
            counts.merge(resolveAgeGroup(age), 1L, Long::sum);
        }

        long total = ages.size();
        return counts.entrySet().stream()
                .map(entry -> new AgeGroupStatResponse(
                        entry.getKey(),
                        entry.getValue(),
                        total == 0 ? 0.0 : Math.round(entry.getValue() * 1000.0 / total) / 10.0
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PhoneNumberStatResponse> getPhoneNumberStats() {
        return phoneNumberRecordRepository.findAllByOrderByDetectionCountDesc().stream()
                .map(PhoneNumberStatResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FrozenAccountStatResponse> getFrozenAccountStats() {
        return frozenAccountRepository.findAllByOrderByRequestedAtDesc().stream()
                .map(FrozenAccountStatResponse::from)
                .toList();
    }

    private String resolveAgeGroup(int age) {
        if (age <= 29) {
            return "20대 이하";
        } else if (age <= 39) {
            return "30대";
        } else if (age <= 49) {
            return "40대";
        } else if (age <= 59) {
            return "50대";
        } else {
            return "60대 이상";
        }
    }
}
