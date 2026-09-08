package com.example.conferenceservice.common.security;

import com.example.conferenceservice.common.exception.BusinessException;
import com.example.conferenceservice.common.exception.ErrorCode;

import java.util.UUID;

/**
 * 요청자와 리소스 소유자(organizerId 등)를 비교해 불일치 시 지정된 ErrorCode(403)로 거부한다.
 * Task 7-2에서 도입, Task 8-2 공통 인가 필터에서도 재사용된다.
 */
public final class OwnerScopeGuard {

    private OwnerScopeGuard() {
    }

    public static void verify(UUID requesterId, UUID resourceOwnerId, ErrorCode deniedErrorCode) {
        if (requesterId == null || !requesterId.equals(resourceOwnerId)) {
            throw new BusinessException(deniedErrorCode);
        }
    }
}
