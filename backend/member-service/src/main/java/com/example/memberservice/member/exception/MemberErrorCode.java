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
    BUSINESS_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "MEMBER_BUSINESS_NOT_REGISTERED", "국세청에 등록되지 않은 사업자등록번호입니다. 번호를 다시 확인해주세요."),
    BUSINESS_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "MEMBER_BUSINESS_NOT_ACTIVE", "휴업 또는 폐업한 사업자등록번호는 사용할 수 없습니다."),
    BUSINESS_VERIFY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "MEMBER_BUSINESS_VERIFY_UNAVAILABLE", "사업자 정보 확인 서비스에 일시적으로 접근할 수 없습니다. 잠시 후 다시 시도해주세요."),
    DUPLICATE_BUSINESS_NO(HttpStatus.CONFLICT, "MEMBER_DUPLICATE_BUSINESS_NO", "이미 등록된 사업자등록번호입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "MEMBER_EMAIL_NOT_VERIFIED", "이메일 인증을 먼저 완료해주세요."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}