package com.ssairen.backend.domain.user.service;

import com.ssairen.backend.domain.user.dto.FcmTokenUpdateResponse;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public FcmTokenUpdateResponse updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.updateFcmToken(fcmToken);

        return new FcmTokenUpdateResponse(true);
    }
}