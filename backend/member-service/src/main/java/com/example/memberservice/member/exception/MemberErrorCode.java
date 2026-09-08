package com.example.memberservice.member.exception;

import com.example.memberservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    INVALID_BUSINESS_NO(HttpStatus.BAD_REQUEST, "MEMBER_INVALID_BUSINESS_NO", "사업자등록번호 형식이 올바르지 않습니다."),
    DUPLICATE_BUSINESS_NO(HttpStatus.CONFLICT, "MEMBER_DUPLICATE_BUSINESS_NO", "이미 등록된 사업자등록번호입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "MEMBER_EMAIL_NOT_VERIFIED", "이메일 인증을 먼저 완료해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}