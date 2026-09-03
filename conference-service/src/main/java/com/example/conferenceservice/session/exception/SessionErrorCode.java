package com.example.conferenceservice.session.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "존재하지 않는 세션입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
