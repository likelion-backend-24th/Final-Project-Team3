package com.example.conferenceservice.operationstatus.exception;

import com.example.conferenceservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OperationStatusErrorCode implements ErrorCode {

    RESERVATION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "RESERVATION_SERVICE_UNAVAILABLE", "신청·입장 현황 조회 서비스에 일시적으로 연결할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
