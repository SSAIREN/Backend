package com.ssairen.backend.domain.pairing.service;

import com.ssairen.backend.domain.pairing.dto.GuardianResponse;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PairingService {

    private final PairingRepository pairingRepository;
    private final UserRepository userRepository;

    public PairingService(PairingRepository pairingRepository, UserRepository userRepository) {
        this.pairingRepository = pairingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GuardianResponse> getGuardians(Long victimId) {
        userRepository.findByIdAndRole(victimId, UserRole.VICTIM)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "피해자를 찾을 수 없습니다."));

        return pairingRepository.findAllByVictimId(victimId).stream()
                .map(pairing -> GuardianResponse.from(pairing.getGuardian()))
                .toList();
    }
}