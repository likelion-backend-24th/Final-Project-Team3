package com.example.memberservice.auth.exception;

import com.example.memberservice.common.exception.BusinessException;

// Refresh Token 재사용(탈취 의심)을 감지했을 때 던지는 예외.
// 감지 시 회원의 모든 Refresh Token을 폐기한 뒤 던지는데, 그 폐기는 롤백되면 안 되므로
// 트랜잭션 경계(RefreshTokenService.rotate(), AuthService.reissue())에서 noRollbackFor로 이 타입만 지정한다.
public class RefreshTokenReusedException extends BusinessException {

    public RefreshTokenReusedException() {
        super(AuthErrorCode.REFRESH_TOKEN_REUSED);
    }
}
