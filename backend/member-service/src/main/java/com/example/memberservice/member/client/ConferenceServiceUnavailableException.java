package com.example.memberservice.member.client;

import java.util.UUID;

// Conference-Service 호출 실패(타임아웃·5xx·네트워크 오류 등)를 감싸는 런타임 예외.
// 호출부(WithdrawalService)가 이걸 잡아서 fail-closed 정책(탈퇴 차단)으로 변환한다.
public class ConferenceServiceUnavailableException extends RuntimeException {
    public ConferenceServiceUnavailableException(UUID organizerId, Throwable cause) {
        super("Conference-Service 탈퇴 가능 여부 조회 실패: organizerId=" + organizerId, cause);
    }
}
