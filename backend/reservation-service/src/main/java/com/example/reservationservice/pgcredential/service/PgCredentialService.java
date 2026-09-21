package com.example.reservationservice.pgcredential.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.dto.PgConfigResponse;
import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.dto.PgCredentialResponse;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PgCredentialService {

    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    private final PgCredentialRepository pgCredentialRepository;
    private final PgCredentialEncryptor encryptor;

    @Transactional
    public PgCredentialResponse registerOrUpdate(PgCredentialRequest request) {
        String encryptedApiSecret = encryptor.encrypt(request.apiSecret());
        String encryptedWebhookSecret = encryptor.encrypt(request.webhookSecret());

        PgCredential credential = pgCredentialRepository.findByProvider(request.provider())
                .map(existing -> {
                    existing.update(request.storeId(), request.channelKey(), encryptedApiSecret, encryptedWebhookSecret);
                    return existing;
                })
                .orElseGet(() -> pgCredentialRepository.save(
                        PgCredential.builder()
                                .provider(request.provider())
                                .storeId(request.storeId())
                                .channelKey(request.channelKey())
                                .apiSecret(encryptedApiSecret)
                                .webhookSecret(encryptedWebhookSecret)
                                .build()
                ));

        return PgCredentialResponse.of(credential, mask(request.apiSecret()), mask(request.webhookSecret()));
    }

    @Transactional(readOnly = true)
    public PgConfigResponse getPublicConfig(String provider) {
        PgCredential credential = pgCredentialRepository.findByProvider(provider)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.PG_CONFIG_NOT_FOUND));
        return PgConfigResponse.from(credential);
    }

    private String mask(String plainSecret) {
        if (plainSecret.length() <= VISIBLE_SUFFIX_LENGTH) {
            return "*".repeat(plainSecret.length());
        }
        String suffix = plainSecret.substring(plainSecret.length() - VISIBLE_SUFFIX_LENGTH);
        return "*".repeat(plainSecret.length() - VISIBLE_SUFFIX_LENGTH) + suffix;
    }
}
