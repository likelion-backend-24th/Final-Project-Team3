package com.example.reservationservice.pgcredential.dto;

import com.example.reservationservice.pgcredential.entity.PgCredential;
import io.swagger.v3.oas.annotations.media.Schema;

// 참가자 결제창(PortOne SDK) 초기화용 공개 설정. apiSecret/webhookSecret은 절대 포함하지 않는다.
@Schema(description = "결제창 초기화용 PG 공개 설정")
public record PgConfigResponse(
        @Schema(description = "PG 제공자", example = "PORTONE") String provider,
        @Schema(description = "PortOne 상점 ID(SDK의 storeId)", example = "store-xxxxxxxx") String storeId,
        @Schema(description = "PortOne 결제 채널 키(SDK의 channelKey)", example = "channel-key-xxxxxxxx") String channelKey
) {
    public static PgConfigResponse from(PgCredential credential) {
        return new PgConfigResponse(credential.getProvider(), credential.getStoreId(), credential.getChannelKey());
    }
}
