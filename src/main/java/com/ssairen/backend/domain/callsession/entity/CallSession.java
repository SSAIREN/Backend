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

    /*
     * call_session은 ERD의 핵심 비즈니스 엔티티라기보다
     * Flutter WebSocket 통신을 안정적으로 유지하기 위한 기술 세션이다.
     * 실제 사용자 정보는 users, 실제 탐지 데이터는 cases에 저장하고 여기서는 둘을 연결한다.
     */
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String externalCallId;

    @Column(nullable = false, length = 100)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "victim_id", nullable = false)
    private User victim;

    /*
     * 현재 구현에서는 세션 1개가 case 1개와 1:1로 연결된다.
     * 추후 요구사항이 바뀌면 한 통화 안에서 여러 case가 생기도록 바꿀 수 있다.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private FraudCase fraudCase;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallSessionStatus status;

    // 서버가 다음에 받아야 하는 STT 청크 순서
    @Column(nullable = false)
    private long nextTranscriptSequence;

    // 누적 문자 수 기반 분석 트리거를 위한 카운터
    @Column(nullable = false)
    private long accumulatedTranscriptCharacters;

    // 마지막 분석 요청이 걸린 sequence 위치
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
        // 청크가 저장된 뒤에만 sequence를 증가시킨다.
        this.nextTranscriptSequence++;
        this.accumulatedTranscriptCharacters += textLength;
        this.updatedAt = OffsetDateTime.now();

        // 세션에 들어온 마지막 청크 기준으로 case의 통화 진행 시간도 같이 갱신한다.
        this.fraudCase.updateTranscriptProgress(endedAtMs);
    }

    public boolean queueFinalAnalysisIfNeeded(long lastTranscriptSequence) {
        // 이미 큐잉한 범위 이하라면 중복 종료 요청으로 보고 무시한다.
        if (lastTranscriptSequence <= 0 || lastTranscriptSequence <= this.lastAnalysisRequestedSequence) {
            return false;
        }

        this.lastAnalysisRequestedSequence = lastTranscriptSequence;
        this.finalAnalysisRequestedAt = OffsetDateTime.now();
        this.updatedAt = this.finalAnalysisRequestedAt;
        return true;
    }

    public void complete(OffsetDateTime endedAt) {
        // 통화 세션 종료와 case 종료는 함께 가야 한다.
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
