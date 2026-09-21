package com.example.reservationservice.pgcredential.dto;

import com.example.reservationservice.pgcredential.entity.PgCredential;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "PG 연동 키 저장 결과. 시크릿은 항상 마스킹되어 나간다")
public record PgCredentialResponse(
        @Schema(description = "PG 제공자", example = "PORTONE") String provider,
        @Schema(description = "PortOne 상점 ID", example = "store-xxxxxxxx") String storeId,
        @Schema(description = "PortOne 결제 채널 키", example = "channel-key-xxxxxxxx") String channelKey,
        @Schema(description = "마스킹된 API Secret(뒤 4자리만 표시)", example = "************abcd") String apiSecret,
        @Schema(description = "마스킹된 웹훅 시크릿(뒤 4자리만 표시)", example = "************abcd") String webhookSecret,
        @Schema(description = "마지막 저장 시각") LocalDateTime updatedAt
) {
    public static PgCredentialResponse of(PgCredential credential, String maskedApiSecret, String maskedWebhookSecret) {
        return new PgCredentialResponse(
                credential.getProvider(),
                credential.getStoreId(),
                credential.getChannelKey(),
                maskedApiSecret,
                maskedWebhookSecret,
                credential.getUpdatedAt()
        );
    }
}
