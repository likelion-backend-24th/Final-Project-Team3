package com.example.reservationservice.qrticket.exception;

import com.example.reservationservice.common.exception.BusinessException;

public class QrTicketException extends BusinessException {
    public QrTicketException(QrTicketErrorCode errorCode) {
        super(errorCode);
    }
}
