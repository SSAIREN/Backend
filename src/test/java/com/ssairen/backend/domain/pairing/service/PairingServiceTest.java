package com.ssairen.backend.domain.pairing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ssairen.backend.domain.pairing.dto.GuardianResponse;
import com.ssairen.backend.domain.pairing.entity.Pairing;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PairingServiceTest {

    @Mock
    private PairingRepository pairingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PairingService pairingService;

    @Test
    void 피해자와_페어링된_보호자_목록을_조회한다() {
        User victim = new User("김영희", UserRole.VICTIM, 71, "01012345678");
        ReflectionTestUtils.setField(victim, "id", 1001L);
        User guardian = new User("김민수", UserRole.GUARDIAN, 45, "01077778888");
        ReflectionTestUtils.setField(guardian, "id", 2001L);
        guardian.updateFcmToken("dummy-guardian-fcm-token-2001");

        given(userRepository.findByIdAndRole(1001L, UserRole.VICTIM)).willReturn(Optional.of(victim));
        given(pairingRepository.findAllByVictimId(1001L)).willReturn(List.of(new Pairing(victim, guardian)));

        List<GuardianResponse> result = pairingService.getGuardians(1001L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).guardianId()).isEqualTo(2001L);
        assertThat(result.get(0).name()).isEqualTo("김민수");
        assertThat(result.get(0).fcmToken()).isEqualTo("dummy-guardian-fcm-token-2001");
    }

    @Test
    void 존재하지_않는_피해자의_보호자_목록을_조회하면_예외가_발생한다() {
        given(userRepository.findByIdAndRole(9999L, UserRole.VICTIM)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pairingService.getGuardians(9999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}