package com.ssairen.backend.domain.casefile.repository;

import com.ssairen.backend.domain.casefile.entity.FraudCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {
}
