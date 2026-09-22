package com.example.reservationservice.pgcredential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "PG 연동 키 등록·수정 요청")
public record PgCredentialRequest(
        @Schema(description = "PG 제공자(현재 PORTONE만 사용)", example = "PORTONE") @NotBlank String provider,
        @Schema(description = "PortOne 상점 ID", example = "store-xxxxxxxx") @NotBlank String storeId,
        @Schema(description = "PortOne 결제 채널 키(일반 결제 채널)", example = "channel-key-xxxxxxxx") @NotBlank String channelKey,
        @Schema(description = "PortOne V2 API Secret. 저장 시 암호화되며 조회 응답에는 마스킹된다") @NotBlank String apiSecret,
        @Schema(description = "웹훅 서명 검증용 시크릿. 저장 시 암호화되며 조회 응답에는 마스킹된다") @NotBlank String webhookSecret
) {}
