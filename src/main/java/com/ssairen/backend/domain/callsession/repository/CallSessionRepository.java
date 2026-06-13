package com.ssairen.backend.domain.callsession.repository;

import com.ssairen.backend.domain.callsession.entity.CallSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CallSessionRepository extends JpaRepository<CallSession, String> {

    Optional<CallSession> findByExternalCallId(String externalCallId);

    /**
     * 같은 통화 세션의 WebSocket 메시지가 동시에 처리되어도 sequence가 한 번씩만 증가하도록
     * DB 비관적 잠금을 사용한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CallSession c where c.id = :id")
    Optional<CallSession> findByIdForUpdate(@Param("id") String id);
}
