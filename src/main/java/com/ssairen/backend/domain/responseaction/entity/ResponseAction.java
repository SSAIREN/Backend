package com.ssairen.backend.domain.responseaction.entity;

import com.ssairen.backend.domain.casefile.entity.FraudCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "response_actions")
public class ResponseAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private FraudCase fraudCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ResponseActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResponseActionStatus status;

    @Column(columnDefinition = "text")
    private String result;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    protected ResponseAction() {
    }

    public ResponseAction(FraudCase fraudCase, ResponseActionType actionType) {
        this.fraudCase = fraudCase;
        this.actionType = actionType;
        this.status = ResponseActionStatus.PENDING;
    }
}
