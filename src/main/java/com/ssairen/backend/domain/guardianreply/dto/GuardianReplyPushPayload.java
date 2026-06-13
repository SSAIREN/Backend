package com.ssairen.backend.domain.guardianreply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "피해자 앱으로 전달되는 보호자 응답 payload")
public record GuardianReplyPushPayload(
        @Schema(description = "보호자 사용자 ID", example = "2001")
        Long guardianId,

        @Schema(description = "보호자 이름", example = "홍길동")
        String guardianName,

        @Schema(description = "응답이 연결된 사기 케이스 ID", example = "501")
        Long caseId,

        @Schema(description = "보호자 현재 위도", example = "37.4979")
        Double latitude,

        @Schema(description = "보호자 현재 경도", example = "127.0276")
        Double longitude,

        @Schema(description = "보호자가 입력한 메시지", example = "저 안전해요, 그 전화 끊으세요")
        String message,

        @Schema(description = "응답 수신 시각", example = "2026-06-14T16:20:47+09:00")
        OffsetDateTime receivedAt
) {
}
