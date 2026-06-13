package com.ssairen.backend.domain.frozenaccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

@Entity
@Table(name = "frozen_accounts")
@Getter
public class FrozenAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(name = "bank_name", nullable = false, length = 20)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FrozenAccountStatus status;

    protected FrozenAccount() {
    }

    public FrozenAccount(String bankName, String accountNumber) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.status = FrozenAccountStatus.REQUESTED;
    }
}
