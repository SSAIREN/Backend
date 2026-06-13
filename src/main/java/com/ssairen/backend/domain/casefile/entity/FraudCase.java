package com.ssairen.backend.domain.casefile.entity;

import com.ssairen.backend.domain.user.entity.User;
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
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
public class FraudCase {

    /*
     * cases 는 ERD 기준 핵심 사건 엔티티다.
     * 최근 transcript 분석 결과가 누적 반영되며,
     * 운영 DB 컬럼명과 정확히 맞도록 전부 명시적으로 고정한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "victim_id", nullable = false)
    private User victim;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "phishing_type", length = 50)
    private PhishingType phishingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseStatus status;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Column(name = "keywords", length = 255)
    private String keywords;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "call_duration_sec")
    private Integer callDurationSec;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    protected FraudCase() {
    }

    public FraudCase(User victim, OffsetDateTime detectedAt) {
        this.victim = victim;
        this.riskScore = 0;
        this.status = CaseStatus.IN_PROGRESS;
        this.detectedAt = detectedAt;
    }

    public void updateTranscriptProgress(long endedAtMs) {
        int progressedSeconds = (int) Math.ceil(endedAtMs / 1000.0d);
        if (this.callDurationSec == null || progressedSeconds > this.callDurationSec) {
            this.callDurationSec = progressedSeconds;
        }
    }

    public void applyAnalysisResult(
            Integer riskScore,
            PhishingType phishingType,
            String aiSummary,
            List<String> keywords
    ) {
        if (riskScore != null) {
            this.riskScore = Math.max(0, Math.min(100, riskScore));
        }
        this.phishingType = phishingType;
        this.aiSummary = aiSummary;
        this.keywords = (keywords == null || keywords.isEmpty()) ? null : String.join(",", keywords);
    }

    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void updateStatus(CaseStatus status, OffsetDateTime changedAt) {
        /*
         * 대시보드에서의 수동 상태 변경.
         * 완료로 전환되면 응답 시각이 비어있을 때만 채우고,
         * 다시 진행중으로 되돌리면 응답 시각을 초기화한다.
         */
        this.status = status;
        if (status == CaseStatus.COMPLETED) {
            if (this.respondedAt == null) {
                this.respondedAt = changedAt;
            }
        } else {
            this.respondedAt = null;
        }
    }

    public void complete(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        this.status = CaseStatus.COMPLETED;
        this.respondedAt = endedAt;

        long durationSeconds = Math.max(0L, Duration.between(startedAt, endedAt).getSeconds());
        int completedDuration = Math.toIntExact(durationSeconds);
        if (this.callDurationSec == null || completedDuration > this.callDurationSec) {
            this.callDurationSec = completedDuration;
        }
    }
}
