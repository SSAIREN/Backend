package com.ssairen.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    CALL_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND),
    CALL_SESSION_COMPLETED(HttpStatus.CONFLICT),
    TRANSCRIPT_SEQUENCE_MISMATCH(HttpStatus.CONFLICT),
    DUPLICATE_TRANSCRIPT_CONFLICT(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
