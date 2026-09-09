package com.example.conferenceservice.faq.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FaqErrorCode implements ErrorCode {

    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FAQ_NOT_FOUND", "존재하지 않는 FAQ입니다."),
    FAQ_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FAQ_ACCESS_DENIED", "본인이 등록한 컨퍼런스의 FAQ만 관리할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
