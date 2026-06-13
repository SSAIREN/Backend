package com.ssairen.backend.domain.casefile.dto;

import com.ssairen.backend.domain.casefile.entity.CaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "케이스 상태 변경 요청")
public record CaseStatusUpdateRequest(
        @Schema(description = "변경할 케이스 상태", example = "COMPLETED")
        @NotNull
        CaseStatus status
) {
}
