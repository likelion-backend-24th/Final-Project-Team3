package com.example.reservationservice.reservation.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@EqualsAndHashCode
public class ActiveReservationLockId implements Serializable {
    private UUID sessionId;
    private UUID memberId;
}