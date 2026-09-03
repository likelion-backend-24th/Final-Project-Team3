package com.example.memberservice.auth.security;

import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.common.exception.BusinessException;

import java.util.UUID;

// 주최자가 자신이 소유하지 않은 자원에 접근하는 것을 막는 공통 검증 로직.
// JWT의 organizerId claim과 자원의 소유자 id를 비교만 하는 순수 로직이라, 다른 서비스에서도 그대로 가져다 쓸 수 있음.
public class OwnerScopeGuard {
    private OwnerScopeGuard() {}

    public static void checkOwnership(UUID callerOrganizerId, UUID resourceOwnerId) {
        if(callerOrganizerId == null || !callerOrganizerId.equals(resourceOwnerId)) {
            throw new BusinessException(AuthErrorCode.ORGANIZER_SCOPE_FORBIDDEN);
        }
    }
}
