package com.ssairen.backend.global.config;

import com.ssairen.backend.domain.pairing.entity.Pairing;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MvpDummyDataInitializer {

    @Bean
    ApplicationRunner seedMvpUsers(
            UserRepository userRepository,
            PairingRepository pairingRepository
    ) {
        return args -> {
            /*
             * MVP 단계에서는 Flutter가 하드코딩된 userId를 보내는 전제를 둔다.
             * 따라서 서버 기동 시 피해자 / 보호자 / pairing 더미 데이터를 미리 심어 둔다.
             */
            User victim = userRepository.findById(1001L)
                    .orElseGet(() -> userRepository.save(User.mvpPreset(
                            1001L,
                            "김영희",
                            UserRole.VICTIM,
                            71,
                            "01012345678",
                            null,
                            OffsetDateTime.now()
                    )));

            User guardian = userRepository.findById(2001L)
                    .orElseGet(() -> userRepository.save(User.mvpPreset(
                            2001L,
                            "김민수",
                            UserRole.GUARDIAN,
                            45,
                            "01077778888",
                            "dummy-guardian-fcm-token-2001",
                            OffsetDateTime.now()
                    )));

            if (!pairingRepository.existsByVictimIdAndGuardianId(victim.getId(), guardian.getId())) {
                pairingRepository.save(new Pairing(victim, guardian));
            }
        };
    }
}
