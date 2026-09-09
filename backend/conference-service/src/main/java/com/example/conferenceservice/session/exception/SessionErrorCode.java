package com.example.conferenceservice.session.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "존재하지 않는 세션입니다."),
    INVALID_SESSION_CAPACITY(HttpStatus.BAD_REQUEST, "INVALID_SESSION_CAPACITY", "세션 정원은 1명 이상이어야 합니다."),
    INVALID_SESSION_PERIOD(HttpStatus.BAD_REQUEST, "INVALID_SESSION_PERIOD", "신청 종료일은 시작일보다 이후여야 합니다."),
    INVALID_SESSION_SCHEDULE(HttpStatus.BAD_REQUEST, "INVALID_SESSION_SCHEDULE", "세션 진행 종료 일시는 시작 일시보다 이후여야 합니다."),
    INVALID_SESSION_SCHEDULE_BEFORE_REGISTRATION(HttpStatus.BAD_REQUEST, "INVALID_SESSION_SCHEDULE_BEFORE_REGISTRATION", "세션 진행 시작 일시는 신청 종료 일시보다 이후여야 합니다."),
    CONFERENCE_NOT_APPROVED(HttpStatus.CONFLICT, "CONFERENCE_NOT_APPROVED", "승인된 컨퍼런스에만 세션을 등록할 수 있습니다."),
    SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SESSION_ACCESS_DENIED", "본인이 등록한 컨퍼런스의 세션만 관리할 수 있습니다."),
    SESSION_ALREADY_DECIDED(HttpStatus.CONFLICT, "SESSION_ALREADY_DECIDED", "이미 처리된 세션입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
