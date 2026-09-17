package com.example.conferenceservice.attendeesummary.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AttendeeSummaryErrorCode implements ErrorCode {

    RESERVATION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "RESERVATION_SERVICE_UNAVAILABLE", "참석자 통계 조회 서비스에 일시적으로 연결할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
