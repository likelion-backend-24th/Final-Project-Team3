package com.example.reservationservice.qrticket.exception;

import com.example.reservationservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QrTicketErrorCode implements ErrorCode {

    QR_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "QR_TICKET_NOT_FOUND", "존재하지 않는 QR 티켓입니다."),
    QR_TICKET_ALREADY_USED(HttpStatus.CONFLICT, "QR_TICKET_ALREADY_USED", "이미 사용된 QR 티켓입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}