package com.example.reservationservice.pgcredential.service;

import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.dto.PgCredentialResponse;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
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
        String encryptedSecretKey = encryptor.encrypt(request.secretKey());

        PgCredential credential = pgCredentialRepository.findByProvider(request.provider())
                .map(existing -> {
                    existing.update(request.apiKey(), encryptedSecretKey);
                    return existing;
                })
                .orElseGet(() -> pgCredentialRepository.save(
                        PgCredential.builder()
                                .provider(request.provider())
                                .apiKey(request.apiKey())
                                .secretKey(encryptedSecretKey)
                                .build()
                ));

        return PgCredentialResponse.of(credential, mask(request.secretKey()));
    }

    private String mask(String plainSecretKey) {
        if (plainSecretKey.length() <= VISIBLE_SUFFIX_LENGTH) {
            return "*".repeat(plainSecretKey.length());
        }
        String suffix = plainSecretKey.substring(plainSecretKey.length() - VISIBLE_SUFFIX_LENGTH);
        return "*".repeat(plainSecretKey.length() - VISIBLE_SUFFIX_LENGTH) + suffix;
    }
}
