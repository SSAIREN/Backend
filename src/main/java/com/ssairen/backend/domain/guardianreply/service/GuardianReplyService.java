package com.ssairen.backend.domain.guardianreply.service;

import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.CallSessionStatus;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.callsession.websocket.VictimNotificationService;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.guardianreply.dto.GuardianReplyPushPayload;
import com.ssairen.backend.domain.guardianreply.dto.GuardianReplyRequest;
import com.ssairen.backend.domain.guardianreply.dto.GuardianReplyResponse;
import com.ssairen.backend.domain.guardianreply.entity.GuardianReply;
import com.ssairen.backend.domain.guardianreply.repository.GuardianReplyRepository;
import com.ssairen.backend.domain.pairing.entity.Pairing;
import com.ssairen.backend.domain.pairing.repository.PairingRepository;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardianReplyService {

    private static final List<CallSessionStatus> ONGOING_STATUSES = List.of(
            CallSessionStatus.ACTIVE,
            CallSessionStatus.COMPLETING
    );

    private final UserRepository userRepository;
    private final PairingRepository pairingRepository;
    private final CallSessionRepository callSessionRepository;
    private final GuardianReplyRepository guardianReplyRepository;
    private final VictimNotificationService victimNotificationService;

    public GuardianReplyService(
            UserRepository userRepository,
            PairingRepository pairingRepository,
            CallSessionRepository callSessionRepository,
            GuardianReplyRepository guardianReplyRepository,
            VictimNotificationService victimNotificationService
    ) {
        this.userRepository = userRepository;
        this.pairingRepository = pairingRepository;
        this.callSessionRepository = callSessionRepository;
        this.guardianReplyRepository = guardianReplyRepository;
        this.victimNotificationService = victimNotificationService;
    }

    @Transactional
    public GuardianReplyResponse createReply(GuardianReplyRequest request) {
        User guardian = userRepository.findByIdAndRole(request.guardianId(), UserRole.GUARDIAN)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND,
                        "보호자 사용자를 찾을 수 없습니다.",
                        Map.of("guardianId", request.guardianId())
                ));

        List<Pairing> pairings = pairingRepository.findAllByGuardianId(guardian.getId());
        if (pairings.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "연결된 피해자가 없는 보호자입니다.",
                    Map.of("guardianId", guardian.getId())
            );
        }

        List<Long> victimIds = pairings.stream()
                .map(pairing -> pairing.getVictim().getId())
                .distinct()
                .toList();

        CallSession callSession = callSessionRepository.findTopByVictimIdInAndStatusInOrderByStartedAtDesc(victimIds, ONGOING_STATUSES)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CALL_SESSION_NOT_FOUND,
                        "현재 진행 중인 피해자 통화 세션을 찾을 수 없습니다.",
                        Map.of("guardianId", guardian.getId(), "victimIds", victimIds)
                ));

        FraudCase fraudCase = callSession.getFraudCase();
        fraudCase.updateLocation(
                BigDecimal.valueOf(request.latitude()),
                BigDecimal.valueOf(request.longitude())
        );

        GuardianReply reply = guardianReplyRepository.save(new GuardianReply(
                callSession,
                fraudCase,
                callSession.getVictim(),
                guardian,
                BigDecimal.valueOf(request.latitude()),
                BigDecimal.valueOf(request.longitude()),
                request.message().trim()
        ));

        GuardianReplyPushPayload payload = new GuardianReplyPushPayload(
                guardian.getId(),
                guardian.getName(),
                fraudCase.getId(),
                request.latitude(),
                request.longitude(),
                request.message().trim(),
                reply.getCreatedAt()
        );
        victimNotificationService.pushEvent(callSession.getId(), "HARMFUL_RESPONSE_UPDATE", payload);

        return new GuardianReplyResponse(
                callSession.getId(),
                fraudCase.getId(),
                callSession.getVictim().getId(),
                guardian.getId(),
                reply.getId(),
                reply.getCreatedAt()
        );
    }
}
