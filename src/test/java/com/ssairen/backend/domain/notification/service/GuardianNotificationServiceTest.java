package com.ssairen.backend.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.domain.casefile.repository.FraudCaseRepository;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationCommand;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationRequest;
import com.ssairen.backend.domain.notification.dto.GuardianNotificationResponse;
import com.ssairen.backend.domain.pairing.entity.Pairing;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuardianNotificationServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private PairingRepository pairingRepository;

    @Mock
    private FcmPushGateway fcmPushGateway;

    @InjectMocks
    private GuardianNotificationService guardianNotificationService;

    @Test
    void FCM_토큰이_있는_보호자에게_알림을_발송하면_성공으로_기록된다() {
        User victim = new User("김영희", UserRole.VICTIM, 71, "01012345678");
        ReflectionTestUtils.setField(victim, "id", 1001L);
        User guardian = new User("김민수", UserRole.GUARDIAN, 45, "01077778888");
        ReflectionTestUtils.setField(guardian, "id", 2001L);
        guardian.updateFcmToken("dummy-guardian-fcm-token-2001");

        given(fraudCaseRepository.findById(1L)).willReturn(Optional.of(new FraudCase(victim, OffsetDateTime.now())));
        given(pairingRepository.findAllByVictimId(1001L)).willReturn(List.of(new Pairing(victim, guardian)));
        given(fcmPushGateway.sendGuardianNotification(any(GuardianNotificationCommand.class))).willReturn(true);

        GuardianNotificationRequest request = new GuardianNotificationRequest(
                1L, 1001L, PhishingType.AGENCY_IMPERSONATION, "검찰 사칭형 보이스피싱이 의심됩니다."
        );

        GuardianNotificationResponse response = guardianNotificationService.sendGuardianNotification(request);

        assertThat(response.sent()).hasSize(1);
        assertThat(response.sent().get(0).guardianId()).isEqualTo(2001L);
        assertThat(response.sent().get(0).success()).isTrue();
        assertThat(response.failCount()).isEqualTo(0);
    }

    @Test
    void FCM_토큰이_없는_보호자는_발송에_실패하고_failCount에_포함된다() {
        User victim = new User("김영희", UserRole.VICTIM, 71, "01012345678");
        ReflectionTestUtils.setField(victim, "id", 1001L);
        User guardian = new User("김민수", UserRole.GUARDIAN, 45, "01077778888");
        ReflectionTestUtils.setField(guardian, "id", 2001L);

        given(fraudCaseRepository.findById(1L)).willReturn(Optional.of(new FraudCase(victim, OffsetDateTime.now())));
        given(pairingRepository.findAllByVictimId(1001L)).willReturn(List.of(new Pairing(victim, guardian)));

        GuardianNotificationRequest request = new GuardianNotificationRequest(
                1L, 1001L, PhishingType.AGENCY_IMPERSONATION, "검찰 사칭형 보이스피싱이 의심됩니다."
        );

        GuardianNotificationResponse response = guardianNotificationService.sendGuardianNotification(request);

        assertThat(response.sent()).hasSize(1);
        assertThat(response.sent().get(0).guardianId()).isEqualTo(2001L);
        assertThat(response.sent().get(0).success()).isFalse();
        assertThat(response.failCount()).isEqualTo(1);
        verify(fcmPushGateway, never()).sendGuardianNotification(any());
    }

    @Test
    void 존재하지_않는_케이스로_알림을_발송하면_예외가_발생한다() {
        given(fraudCaseRepository.findById(999L)).willReturn(Optional.empty());

        GuardianNotificationRequest request = new GuardianNotificationRequest(
                999L, 1001L, PhishingType.AGENCY_IMPERSONATION, "검찰 사칭형 보이스피싱이 의심됩니다."
        );

        assertThatThrownBy(() -> guardianNotificationService.sendGuardianNotification(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CASE_NOT_FOUND);
        verify(pairingRepository, never()).findAllByVictimId(any());
    }
}