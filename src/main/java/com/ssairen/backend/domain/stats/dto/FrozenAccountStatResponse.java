package com.ssairen.backend.domain.stats.dto;

import com.ssairen.backend.domain.frozenaccount.entity.FrozenAccount;
import com.ssairen.backend.domain.frozenaccount.entity.FrozenAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "지급정지 계좌 현황")
public record FrozenAccountStatResponse(
        @Schema(description = "은행명", example = "국민은행")
        String bankName,

        @Schema(description = "계좌번호", example = "123456-78-901234")
        String accountNumber,

        @Schema(description = "지급정지 요청 시각", example = "2026-06-10T15:20:00+09:00")
        OffsetDateTime requestedAt,

        @Schema(description = "처리 상태", example = "REQUESTED")
        FrozenAccountStatus status
) {
    public static FrozenAccountStatResponse from(FrozenAccount account) {
        return new FrozenAccountStatResponse(
                account.getBankName(),
                account.getAccountNumber(),
                account.getRequestedAt(),
                account.getStatus()
        );
    }
}
