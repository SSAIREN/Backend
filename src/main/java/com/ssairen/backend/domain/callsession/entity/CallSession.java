package com.ssairen.backend.domain.callsession.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String externalCallId;

    @Column(nullable = false, length = 100)
    private String deviceId;

    @Column(length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String victimName;

    @Column(nullable = false)
    private Integer victimAge;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallSessionStatus status;

    @Column(nullable = false)
    private long nextTranscriptSequence;

    @Column(nullable = false)
    private long accumulatedTranscriptCharacters;

    @Column(nullable = false)
    private long lastAnalysisRequestedSequence;

    private OffsetDateTime finalAnalysisRequestedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected CallSession() {
    }

    public CallSession(
            String id,
            String externalCallId,
            String deviceId,
            String phoneNumber,
            String victimName,
            Integer victimAge,
            OffsetDateTime startedAt
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        this.id = id;
        this.externalCallId = externalCallId;
        this.deviceId = deviceId;
        this.phoneNumber = phoneNumber;
        this.victimName = victimName;
        this.victimAge = victimAge;
        this.startedAt = startedAt;
        this.status = CallSessionStatus.ACTIVE;
        this.nextTranscriptSequence = 1L;
        this.accumulatedTranscriptCharacters = 0L;
        this.lastAnalysisRequestedSequence = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void acceptTranscript(int textLength) {
        this.nextTranscriptSequence++;
        this.accumulatedTranscriptCharacters += textLength;
        this.updatedAt = OffsetDateTime.now();
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

    public void complete(OffsetDateTime endedAt) {
        this.status = CallSessionStatus.COMPLETED;
        this.endedAt = endedAt;
        this.updatedAt = OffsetDateTime.now();
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
