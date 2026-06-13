package com.ssairen.backend.domain.callsession.entity;

import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    @Column(name = "call_session_id", length = 36)
    private String id;

    @Column(name = "external_call_id", nullable = false, unique = true, length = 100)
    private String externalCallId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "victim_id", nullable = false)
    private User victim;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private FraudCase fraudCase;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CallSessionStatus status;

    @Column(name = "next_transcript_sequence", nullable = false)
    private long nextTranscriptSequence;

    @Column(name = "accumulated_transcript_characters", nullable = false)
    private long accumulatedTranscriptCharacters;

    @Column(name = "last_analysis_requested_sequence", nullable = false)
    private long lastAnalysisRequestedSequence;

    @Column(name = "final_analysis_requested_at")
    private OffsetDateTime finalAnalysisRequestedAt;

    @Column(name = "dashboard_case_notified_at")
    private OffsetDateTime dashboardCaseNotifiedAt;

    @Column(name = "police_notified_at")
    private OffsetDateTime policeNotifiedAt;

    @Column(name = "family_gps_notified_at")
    private OffsetDateTime familyGpsNotifiedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version")
    private long version;

    protected CallSession() {
    }

    public CallSession(
            String id,
            String externalCallId,
            String deviceId,
            User victim,
            FraudCase fraudCase,
            OffsetDateTime startedAt
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        this.id = id;
        this.externalCallId = externalCallId;
        this.deviceId = deviceId;
        this.victim = victim;
        this.fraudCase = fraudCase;
        this.startedAt = startedAt;
        this.status = CallSessionStatus.ACTIVE;
        this.nextTranscriptSequence = 1L;
        this.accumulatedTranscriptCharacters = 0L;
        this.lastAnalysisRequestedSequence = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void acceptTranscript(long endedAtMs, int textLength) {
        this.nextTranscriptSequence++;
        this.accumulatedTranscriptCharacters += textLength;
        this.updatedAt = OffsetDateTime.now();
        this.fraudCase.updateTranscriptProgress(endedAtMs);
    }

    public boolean queueFinalAnalysisIfNeeded(long lastTranscriptSequence) {
        if (lastTranscriptSequence <= 0 || lastTranscriptSequence <= this.lastAnalysisRequestedSequence) {
            return false;
        }

        this.lastAnalysisRequestedSequence = lastTranscriptSequence;
        this.finalAnalysisRequestedAt = OffsetDateTime.now();
        this.updatedAt = this.finalAnalysisRequestedAt;
        return true;
    }
  
    public boolean markDashboardCaseNotifiedIfNeeded() {
        if (this.dashboardCaseNotifiedAt != null) {
            return false;
        }
        this.dashboardCaseNotifiedAt = OffsetDateTime.now();
        this.updatedAt = this.dashboardCaseNotifiedAt;
        return true;
    }

    /**
     * 경찰 대시보드 통지를 세션당 한 번만 보내도록 보장한다.
     * 아직 통지하지 않았으면 시각을 기록하고 true, 이미 통지했으면 false 를 반환한다.
     */
    public boolean markPoliceNotifiedIfNeeded() {
        if (this.policeNotifiedAt != null) {
            return false;
        }
        this.policeNotifiedAt = OffsetDateTime.now();
        this.updatedAt = this.policeNotifiedAt;
        return true;
    }

    /**
     * 가족 GPS(보호자 FCM) 발송을 세션당 한 번만 보내도록 보장한다.
     * 아직 발송하지 않았으면 시각을 기록하고 true, 이미 발송했으면 false 를 반환한다.
     */
    public boolean markFamilyGpsNotifiedIfNeeded() {
        if (this.familyGpsNotifiedAt != null) {
            return false;
        }
        this.familyGpsNotifiedAt = OffsetDateTime.now();
        this.updatedAt = this.familyGpsNotifiedAt;
        return true;
    }

    public void complete(OffsetDateTime endedAt) {
        this.status = CallSessionStatus.COMPLETED;
        this.endedAt = endedAt;
        this.updatedAt = OffsetDateTime.now();
        this.fraudCase.complete(this.startedAt, endedAt);
    }

    public boolean isAcceptingTranscript() {
        return this.status == CallSessionStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public String getExternalCallId() {
        return externalCallId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public User getVictim() {
        return victim;
    }

    public FraudCase getFraudCase() {
        return fraudCase;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public CallSessionStatus getStatus() {
        return status;
    }

    public long getNextTranscriptSequence() {
        return nextTranscriptSequence;
    }

    public long getAccumulatedTranscriptCharacters() {
        return accumulatedTranscriptCharacters;
    }
}
