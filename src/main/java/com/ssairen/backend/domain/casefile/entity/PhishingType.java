package com.ssairen.backend.domain.casefile.entity;

import lombok.Getter;

@Getter
public enum PhishingType {
    AGENCY_IMPERSONATION("기관 사칭"),
    ACCOUNT_TRANSFER_INDUCEMENT("계좌 이체 요구"),
    KIDNAPPING_THREAT("납치 협박"),
    REMOTE_APP_INSTALLATION("원격 조정 앱 설치");

    private final String description;

    PhishingType(String description) {
        this.description = description;
    }
}
