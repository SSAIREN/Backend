package com.ssairen.backend.domain.pairing.repository;

import com.ssairen.backend.domain.pairing.entity.Pairing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingRepository extends JpaRepository<Pairing, Long> {

    List<Pairing> findAllByVictimId(Long victimId);

    List<Pairing> findAllByGuardianId(Long guardianId);

    boolean existsByVictimIdAndGuardianId(Long victimId, Long guardianId);
}
