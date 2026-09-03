package com.example.memberservice.auth.security;

import com.example.memberservice.auth.exception.AuthErrorCode;
import com.example.memberservice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 8-2 검증: 주최자가 자신이 소유한 자원에만 접근할 수 있는지 확인하는
 * 순수 로직(OwnerScopeGuard)에 대한 단위 테스트.
 * 실제 Conference-Service API 연결은 이 Task 범위 밖(NOT_RUN) — Story 9/10/11에서 진행.
 */
class OwnerScopeGuardTest {

    @Test
    void 호출자와_자원_소유자의_organizerId가_같으면_통과한다() {
        UUID organizerId = UUID.randomUUID();

        assertThatCode(() -> OwnerScopeGuard.checkOwnership(organizerId, organizerId))
                .doesNotThrowAnyException();
    }

    @Test
    void 호출자와_자원_소유자의_organizerId가_다르면_403으로_거부된다() {
        UUID callerOrganizerId = UUID.randomUUID();
        UUID resourceOwnerId = UUID.randomUUID();

        assertThatThrownBy(() -> OwnerScopeGuard.checkOwnership(callerOrganizerId, resourceOwnerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.ORGANIZER_SCOPE_FORBIDDEN);
    }

    @Test
    void organizerId가_없는_호출자는_거부된다() {
        // MEMBER role처럼 organizerId claim 자체가 없는 경우 (JwtTokenProvider가 null로 넘겨주는 상황)
        UUID resourceOwnerId = UUID.randomUUID();

        assertThatThrownBy(() -> OwnerScopeGuard.checkOwnership(null, resourceOwnerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.ORGANIZER_SCOPE_FORBIDDEN);
    }
}
