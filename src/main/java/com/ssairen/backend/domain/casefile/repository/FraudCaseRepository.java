package com.ssairen.backend.domain.casefile.repository;

import com.ssairen.backend.domain.casefile.entity.CaseStatus;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {

    List<FraudCase> findByStatusOrderByDetectedAtDesc(CaseStatus status);

    List<FraudCase> findAllByOrderByDetectedAtDesc();

    long countByStatus(CaseStatus status);

    long countByRiskScoreGreaterThanEqual(Integer riskScore);

    @Query("select f.keywords from FraudCase f where f.keywords is not null and f.keywords <> ''")
    List<String> findAllKeywords();

    @Query("select f.victim.age from FraudCase f where f.victim.age is not null")
    List<Integer> findAllVictimAges();
}
