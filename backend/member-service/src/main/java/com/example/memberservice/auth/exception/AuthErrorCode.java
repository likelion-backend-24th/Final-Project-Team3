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
    EMAIL_CODE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_EMAIL_CODE_ATTEMPTS_EXCEEDED", "인증 시도 횟수를 초과했습니다. 인증코드를 다시 요청해주세요."),
    EMAIL_CODE_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "AUTH_EMAIL_CODE_RESEND_TOO_SOON", "잠시 후 다시 시도해주세요."),
    SOCIAL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_SOCIAL_TOKEN_INVALID", "소셜 로그인 인증에 실패했습니다."),
    SOCIAL_EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "AUTH_SOCIAL_EMAIL_ALREADY_REGISTERED", "이미 이메일로 가입된 계정입니다. 비밀번호로 로그인한 뒤 마이페이지에서 계정을 연동해주세요."),
    SOCIAL_EMAIL_NOT_LINKABLE(HttpStatus.CONFLICT, "AUTH_SOCIAL_EMAIL_NOT_LINKABLE", "이 이메일은 참석자(MEMBER) 계정이 아니라서 소셜 로그인을 연결할 수 없습니다. 이메일과 비밀번호로 로그인해주세요."),
    SOCIAL_PROVIDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "AUTH_SOCIAL_PROVIDER_UNSUPPORTED", "지원하지 않는 소셜 로그인입니다."),
    SOCIAL_PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_SOCIAL_PROFILE_REQUIRED", "연령대·직무 정보가 필요합니다."),
    SOCIAL_LINK_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_SOCIAL_LINK_EMAIL_MISMATCH", "연동하려는 소셜 계정의 이메일이 로그인 중인 계정과 다릅니다."),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED", "이미 다른 계정에 연동된 소셜 계정입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}