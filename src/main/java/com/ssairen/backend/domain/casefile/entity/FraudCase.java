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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cases")
public class FraudCase {

    /*
     * ERD 기준의 핵심 비즈니스 엔티티.
     * 실제 탐지 결과, 대응 상태, 위치/요약 정보는 최종적으로 이 테이블에 쌓인다.
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
    @Column(name = "phishing_type", length = 20)
    private PhishingType phishingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Column(name = "ai_summary", columnDefinition = "text")
    private String aiSummary;

    @Column(length = 255)
    private String keywords;

    @Column(length = 50)
    private String region;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
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
        /*
         * 세션 생성 직후에는 아직 분석 결과가 없기 때문에
         * case는 "탐지 시작됨 / 진행 중" 정도의 기본 상태만 가진다.
         */
        this.victim = victim;
        this.riskScore = 0;
        this.status = CaseStatus.IN_PROGRESS;
        this.detectedAt = detectedAt;
    }

    public void updateTranscriptProgress(long endedAtMs) {
        /*
         * STT 청크가 끝난 시점을 기준으로
         * 현재까지의 통화 진행 시간을 초 단위로 추정한다.
         */
        int progressedSeconds = (int) Math.ceil(endedAtMs / 1000.0d);
        if (this.callDurationSec == null || progressedSeconds > this.callDurationSec) {
            this.callDurationSec = progressedSeconds;
        }
    }

    public void complete(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        /*
         * ERD의 responded_at은 현재 구현에서
         * "이 case가 종료 처리된 시각"으로 사용한다.
         */
        this.status = CaseStatus.COMPLETED;
        this.respondedAt = endedAt;

        // 실제 종료 시각 기준의 통화 길이가 더 크면 최종 duration으로 덮어쓴다.
        long durationSeconds = Math.max(0L, Duration.between(startedAt, endedAt).getSeconds());
        int completedDuration = Math.toIntExact(durationSeconds);
        if (this.callDurationSec == null || completedDuration > this.callDurationSec) {
            this.callDurationSec = completedDuration;
        }
    }
}
