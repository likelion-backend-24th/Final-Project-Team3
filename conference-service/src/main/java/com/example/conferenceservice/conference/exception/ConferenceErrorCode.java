package com.example.conferenceservice.conference.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConferenceErrorCode implements ErrorCode {

    CONFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONFERENCE_NOT_FOUND", "존재하지 않는 컨퍼런스입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}