package com.example.reservationservice.review.exception;

import com.example.reservationservice.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_RESERVATION_NOT_FOUND", "존재하지 않는 예약입니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "REVIEW_NOT_OWNER", "본인 예약에 대해서만 후기를 작성할 수 있습니다."),
    NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "REVIEW_NOT_ELIGIBLE", "체크인 완료된 좌석이 있는 예약만 후기를 작성할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
