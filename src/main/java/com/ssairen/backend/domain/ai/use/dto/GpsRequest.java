package com.ssairen.backend.domain.ai.use.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * check_family_gps tool 콜백 바디다. AI가 가족 GPS 위치 확인을 요청한다.
 * 연락처 조회·GPS 조회는 Spring 내부에서 처리한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GpsRequest(
        String callId,
        String userId
) {
}
