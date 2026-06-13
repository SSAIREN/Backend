package com.ssairen.backend.domain.responseaction.repository;

import com.ssairen.backend.domain.responseaction.entity.ResponseAction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseActionRepository extends JpaRepository<ResponseAction, Long> {

    List<ResponseAction> findByFraudCaseIdOrderByExecutedAtAsc(Long fraudCaseId);
}
