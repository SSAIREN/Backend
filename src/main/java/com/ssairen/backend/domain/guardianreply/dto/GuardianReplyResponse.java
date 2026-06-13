package com.ssairen.backend.domain.guardianreply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "보호자 응답 접수 결과")
public record GuardianReplyResponse(
        @Schema(description = "응답이 귀속된 통화 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "사기 케이스 ID", example = "501")
        Long caseId,

        @Schema(description = "피해자 사용자 ID", example = "1001")
        Long victimId,

        @Schema(description = "응답한 보호자 사용자 ID", example = "2001")
        Long guardianId,

        @Schema(description = "저장된 보호자 응답 ID", example = "1")
        Long replyId,

        @Schema(description = "응답 접수 시각", example = "2026-06-14T16:20:47+09:00")
        OffsetDateTime receivedAt
) {
}
