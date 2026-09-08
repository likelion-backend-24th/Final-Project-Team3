package com.example.memberservice.auth.exception;

import com.example.memberservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_NOT_FOUND", "존재하지 않는 토큰입니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_REUSED", "이미 사용된 토큰입니다. 재로그인이 필요합니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_EXPIRED", "만료된 토큰입니다."),
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_MISSING", "리프레시 토큰이 없습니다. 다시 로그인해주세요."),
    ORGANIZER_SCOPE_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_ORGANIZER_SCOPE_FORBIDDEN", "본인이 소유한 자원이 아닙니다."),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST, "AUTH_EMAIL_CODE_INVALID", "인증코드가 올바르지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_EMAIL_CODE_EXPIRED", "인증코드가 만료되었습니다. 다시 요청해주세요."),
    EMAIL_CODE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_EMAIL_CODE_ATTEMPTS_EXCEEDED", "인증 시도 횟수를 초과했습니다. 인증코드를 다시 요청해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}