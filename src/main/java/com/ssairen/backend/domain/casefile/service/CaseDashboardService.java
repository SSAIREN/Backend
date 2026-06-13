package com.ssairen.backend.domain.casefile.service;

import com.ssairen.backend.domain.casefile.dto.CaseDetailResponse;
import com.ssairen.backend.domain.casefile.dto.CaseStatusUpdateResponse;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryMetricsResponse;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryResponse;
import com.ssairen.backend.domain.casefile.dto.ResponseActionResponse;
import com.ssairen.backend.domain.casefile.entity.CaseStatus;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.responseaction.repository.ResponseActionRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.OptionalDouble;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseDashboardService {

    private final FraudCaseRepository fraudCaseRepository;
    private final ResponseActionRepository responseActionRepository;

    public CaseDashboardService(
            FraudCaseRepository fraudCaseRepository,
            ResponseActionRepository responseActionRepository
    ) {
        this.fraudCaseRepository = fraudCaseRepository;
        this.responseActionRepository = responseActionRepository;
    }

    @Transactional(readOnly = true)
    public List<CaseSummaryResponse> getCases(CaseStatus status) {
        List<FraudCase> cases = (status != null)
                ? fraudCaseRepository.findByStatusOrderByDetectedAtDesc(status)
                : fraudCaseRepository.findAllByOrderByDetectedAtDesc();

        return cases.stream()
                .map(CaseSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseDetailResponse getCaseDetail(Long caseId) {
        FraudCase fraudCase = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CASE_NOT_FOUND, "케이스를 찾을 수 없습니다."));

        List<ResponseActionResponse> actions = responseActionRepository.findByFraudCaseIdOrderByExecutedAtAsc(caseId)
                .stream()
                .map(ResponseActionResponse::from)
                .toList();

        return CaseDetailResponse.from(fraudCase, actions);
    }

    @Transactional(readOnly = true)
    public CaseSummaryMetricsResponse getSummaryMetrics() {
        long inProgress = fraudCaseRepository.countByStatus(CaseStatus.IN_PROGRESS);
        List<FraudCase> completedCases = fraudCaseRepository.findByStatusOrderByDetectedAtDesc(CaseStatus.COMPLETED);

        OptionalDouble averageSeconds = completedCases.stream()
                .filter(c -> c.getRespondedAt() != null)
                .mapToLong(c -> Duration.between(c.getDetectedAt(), c.getRespondedAt()).getSeconds())
                .average();
        Long avgResponseSec = averageSeconds.isPresent() ? Math.round(averageSeconds.getAsDouble()) : null;

        return new CaseSummaryMetricsResponse(inProgress, completedCases.size(), avgResponseSec);
    }

    @Transactional
    public CaseStatusUpdateResponse updateStatus(Long caseId, CaseStatus status) {
        FraudCase fraudCase = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CASE_NOT_FOUND, "케이스를 찾을 수 없습니다."));

        fraudCase.updateStatus(status, OffsetDateTime.now());

        return new CaseStatusUpdateResponse(true);
    }
}