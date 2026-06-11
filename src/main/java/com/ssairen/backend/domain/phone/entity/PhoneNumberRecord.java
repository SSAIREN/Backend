package com.ssairen.backend.domain.phone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "phone_numbers")
public class PhoneNumberRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phone_id")
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "detection_count", nullable = false)
    private Integer detectionCount;

    @Column(name = "last_detected_at")
    private OffsetDateTime lastDetectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_status", length = 20)
    private BlockStatus blockStatus;

    protected PhoneNumberRecord() {
    }

    public PhoneNumberRecord(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.detectionCount = 0;
        this.blockStatus = BlockStatus.MONITORING;
    }
}
