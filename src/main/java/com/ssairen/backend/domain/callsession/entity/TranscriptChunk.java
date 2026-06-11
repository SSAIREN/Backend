package com.ssairen.backend.domain.callsession.entity;

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
        name = "transcript_chunks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transcript_chunk_id", columnNames = "chunk_id"),
                @UniqueConstraint(name = "uk_transcript_session_sequence", columnNames = {"call_session_id", "sequence_number"})
        }
)
public class TranscriptChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_chunk_id")
    private Long id;

    @Column(name = "chunk_id", nullable = false, length = 100)
    private String chunkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_session_id", nullable = false)
    private CallSession callSession;

    @Column(name = "sequence_number", nullable = false)
    private long sequence;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "started_at_ms", nullable = false)
    private long startedAtMs;

    @Column(name = "ended_at_ms", nullable = false)
    private long endedAtMs;

    @Column(name = "final_chunk", nullable = false)
    private boolean finalChunk;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    protected TranscriptChunk() {
    }

    public TranscriptChunk(
            String chunkId,
            CallSession callSession,
            long sequence,
            String text,
            long startedAtMs,
            long endedAtMs,
            boolean finalChunk
    ) {
        this.chunkId = chunkId;
        this.callSession = callSession;
        this.sequence = sequence;
        this.text = text;
        this.startedAtMs = startedAtMs;
        this.endedAtMs = endedAtMs;
        this.finalChunk = finalChunk;
        this.receivedAt = OffsetDateTime.now();
    }

    public boolean hasSamePayload(String chunkId, String text) {
        return this.chunkId.equals(chunkId) && this.text.equals(text);
    }

    public String getChunkId() {
        return chunkId;
    }

    public long getSequence() {
        return sequence;
    }
}
