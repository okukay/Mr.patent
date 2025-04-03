package com.d208.mr_patent_backend.global.jwt.exception;

import lombok.Getter;

@Getter
public class JwtException extends RuntimeException {
    private final JwtErrorCode errorCode;
    private final String message;

    public JwtException(JwtErrorCode errorCode) {
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public JwtException(JwtErrorCode errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}