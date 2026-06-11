package com.ssairen.backend.domain.callsession.repository;

import com.ssairen.backend.domain.callsession.entity.TranscriptChunk;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {

    Optional<TranscriptChunk> findByCallSessionIdAndSequence(String callSessionId, long sequence);
}
