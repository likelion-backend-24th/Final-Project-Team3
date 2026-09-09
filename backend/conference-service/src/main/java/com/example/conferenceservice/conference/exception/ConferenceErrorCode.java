package com.example.conferenceservice.conference.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConferenceErrorCode implements ErrorCode {

    CONFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONFERENCE_NOT_FOUND", "존재하지 않는 컨퍼런스입니다."),
    ORGANIZATION_NAME_NOT_FOUND(HttpStatus.UNAUTHORIZED, "ORGANIZATION_NAME_NOT_FOUND", "토큰에 주최기관명 정보가 없습니다. 다시 로그인해주세요."),
    INVALID_CONFERENCE_PERIOD(HttpStatus.BAD_REQUEST, "INVALID_CONFERENCE_PERIOD", "종료 일시는 시작 일시보다 이후여야 합니다."),
    CONFERENCE_ALREADY_DECIDED(HttpStatus.CONFLICT, "CONFERENCE_ALREADY_DECIDED", "이미 처리된 컨퍼런스입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}