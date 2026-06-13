package com.ssairen.backend.domain.phone.repository;

import com.ssairen.backend.domain.phone.entity.PhoneNumberRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneNumberRecordRepository extends JpaRepository<PhoneNumberRecord, Long> {

    List<PhoneNumberRecord> findAllByOrderByDetectionCountDesc();
}
