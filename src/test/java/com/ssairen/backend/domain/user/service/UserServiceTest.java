package com.ssairen.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ssairen.backend.domain.user.dto.FcmTokenUpdateResponse;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 사용자의_FCM_토큰을_등록하면_성공_응답을_반환한다() {
        User user = new User("김민수", UserRole.GUARDIAN, 45, "01077778888");
        ReflectionTestUtils.setField(user, "id", 2001L);
        given(userRepository.findById(2001L)).willReturn(Optional.of(user));

        FcmTokenUpdateResponse response = userService.updateFcmToken(2001L, "new-fcm-token");

        assertThat(response.success()).isTrue();
        assertThat(user.getFcmToken()).isEqualTo("new-fcm-token");
    }

    @Test
    void 존재하지_않는_사용자의_FCM_토큰을_등록하면_예외가_발생한다() {
        given(userRepository.findById(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateFcmToken(9999L, "new-fcm-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}