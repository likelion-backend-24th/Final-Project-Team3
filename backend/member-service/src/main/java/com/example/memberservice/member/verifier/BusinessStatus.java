package com.example.memberservice.member.verifier;

public enum BusinessStatus {
    ACTIVE,          // 계속사업자
    SUSPENDED,       // 휴업자
    CLOSED,          // 폐업자
    NOT_REGISTERED   // 국세청에 등록되지 않은 번호
}
