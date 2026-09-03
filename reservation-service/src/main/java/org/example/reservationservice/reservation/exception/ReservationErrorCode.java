package org.example.reservationservice.reservation.exception;

import org.example.reservationservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_IN_QUEUE(HttpStatus.NOT_FOUND, "RESERVATION_NOT_IN_QUEUE", "대기열에 존재하지 않는 예약입니다."),
    QUEUE_POSITION_NOT_REACHED(HttpStatus.FORBIDDEN, "RESERVATION_QUEUE_POSITION_NOT_REACHED", "대기열 순번이 되지 않아 결제에 진입할 수 없습니다."),
    SESSION_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "RESERVATION_SESSION_CAPACITY_EXCEEDED", "세션 정원이 초과되었습니다."),
    CONFERENCE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CONFERENCE_SERVICE_UNAVAILABLE", "정원 확인 서비스에 일시적으로 연결할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}