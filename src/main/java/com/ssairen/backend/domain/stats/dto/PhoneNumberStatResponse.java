package com.ssairen.backend.domain.stats.dto;

import com.ssairen.backend.domain.phone.entity.BlockStatus;
import com.ssairen.backend.domain.phone.entity.PhoneNumberRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "위험 발신번호 통계")
public record PhoneNumberStatResponse(
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "탐지 횟수", example = "12")
        Integer detectionCount,

        @Schema(description = "마지막 탐지 시각", example = "2026-06-10T15:20:00+09:00")
        OffsetDateTime lastDetectedAt,

        @Schema(description = "차단 상태", example = "MONITORING")
        BlockStatus blockStatus
) {
    public static PhoneNumberStatResponse from(PhoneNumberRecord record) {
        return new PhoneNumberStatResponse(
                record.getPhoneNumber(),
                record.getDetectionCount(),
                record.getLastDetectedAt(),
                record.getBlockStatus()
        );
    }
}
