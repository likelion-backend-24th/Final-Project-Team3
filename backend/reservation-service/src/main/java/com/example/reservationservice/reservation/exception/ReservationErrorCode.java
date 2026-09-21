package com.example.reservationservice.reservation.exception;

import com.example.reservationservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_IN_QUEUE(HttpStatus.NOT_FOUND, "RESERVATION_NOT_IN_QUEUE", "대기열에 존재하지 않는 예약입니다."),
    QUEUE_POSITION_NOT_REACHED(HttpStatus.FORBIDDEN, "RESERVATION_QUEUE_POSITION_NOT_REACHED", "대기열 순번이 되지 않아 결제에 진입할 수 없습니다."),
    SESSION_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "RESERVATION_SESSION_CAPACITY_EXCEEDED", "세션 정원이 초과되었습니다."),
    CONFERENCE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CONFERENCE_SERVICE_UNAVAILABLE", "정원 확인 서비스에 일시적으로 연결할 수 없습니다."),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "RESERVATION_DUPLICATE", "이미 이세션에 신청하셨습니다."),
    ALREADY_CONFIRMED(HttpStatus.CONFLICT, "RESERVATION_ALREADY_CONFIRMED", "이미 결제 완료 예약입니다."),
    PAYMENT_NOT_COMPLETED(HttpStatus.NOT_FOUND, "RESERVATION_PAYMENT_NOT_COMPLETED", "결제 완료되지 않아 QR 티켓을 조회할 수 없습니다."),
    ATTENDEE_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "RESERVATION_ATTENDEE_INFO_REQUIRED", "동반자의 연령대·직무 정보를 모두 입력해야 합니다."),
    ALREADY_CANCELLED(HttpStatus.CONFLICT, "RESERVATION_ALREADY_CANCELLED", "이미 취소된 예약입니다."),
    PAYMENT_NOT_PAID(HttpStatus.PAYMENT_REQUIRED, "RESERVATION_PAYMENT_NOT_PAID", "포트원 결제가 완료되지 않았습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "RESERVATION_PAYMENT_AMOUNT_MISMATCH", "결제 금액이 예약 금액과 일치하지 않습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_PAYMENT_NOT_FOUND", "포트원에서 결제 건을 찾을 수 없습니다."),
    PORTONE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "RESERVATION_PORTONE_API_ERROR", "결제 검증 서비스에 일시적으로 연결할 수 없습니다."),
    PG_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "RESERVATION_PG_NOT_CONFIGURED", "PG 연동 정보가 등록되어 있지 않습니다."),
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RESERVATION_ACCESS_DENIED", "본인의 예약만 취소할 수 있습니다."),
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "RESERVATION_WEBHOOK_SIGNATURE_INVALID", "웹훅 서명 검증에 실패했습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "존재하지 않는 예약입니다."),
    PG_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_PG_CONFIG_NOT_FOUND", "등록된 PG 연동 정보가 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}