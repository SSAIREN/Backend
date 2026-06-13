package com.ssairen.backend.domain.pairing.entity;

import com.ssairen.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "pairings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"victim_id", "guardian_id"})
)
public class Pairing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pairing_id")
    private Long pairingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "victim_id", nullable = false)
    private User victim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private User guardian;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected Pairing() {
    }

    public Pairing(User victim, User guardian) {
        this.victim = victim;
        this.guardian = guardian;
        this.createdAt = OffsetDateTime.now();
    }

    public User getGuardian() {
        return guardian;
    }
}
