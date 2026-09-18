package com.example.reservationservice.pgcredential.dto;

import com.example.reservationservice.pgcredential.entity.PgCredential;

// 참가자 결제창(PortOne SDK) 초기화용 공개 설정. apiSecret/webhookSecret은 절대 포함하지 않는다.
public record PgConfigResponse(String provider, String storeId, String channelKey) {
    public static PgConfigResponse from(PgCredential credential) {
        return new PgConfigResponse(credential.getProvider(), credential.getStoreId(), credential.getChannelKey());
    }
}
