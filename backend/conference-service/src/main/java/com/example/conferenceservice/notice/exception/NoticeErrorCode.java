package com.example.conferenceservice.notice.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements ErrorCode {

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "존재하지 않는 공지입니다."),
    NOTICE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NOTICE_ACCESS_DENIED", "본인이 등록한 컨퍼런스의 공지만 관리할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
