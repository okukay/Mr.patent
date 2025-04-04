package com.d208.mr_patent_backend.global.jwt.exception;

import lombok.Getter;

@Getter
public enum JwtErrorCode {
    TOKEN_EXPIRED("만료된 토큰입니다."),
    TOKEN_MALFORMED("잘못된 형식의 토큰입니다."),
    TOKEN_UNSUPPORTED("지원하지 않는 토큰입니다."),
    TOKEN_ILLEGAL_ARGUMENT("잘못된 토큰입니다."),
    TOKEN_NOT_FOUND("토큰이 존재하지 않습니다."),
    TOKEN_INVALID("유효하지 않은 토큰입니다.");

    private final String message;

    JwtErrorCode(String message) {
        this.message = message;
    }
}