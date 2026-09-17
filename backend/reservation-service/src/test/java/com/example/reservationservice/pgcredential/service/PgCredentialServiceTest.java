package com.example.reservationservice.pgcredential.service;

import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.dto.PgCredentialResponse;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgCredentialServiceTest {

    @Mock
    private PgCredentialRepository pgCredentialRepository;

    private PgCredentialEncryptor encryptor;
    private PgCredentialService pgCredentialService;

    @BeforeEach
    void setUp() {
        encryptor = new PgCredentialEncryptor("test-encryption-key-1234");
        pgCredentialService = new PgCredentialService(pgCredentialRepository, encryptor);
    }

    private PgCredentialRequest request() {
        return new PgCredentialRequest("PORTONE", "store-abc123", "channel-key-toss-general", "apiSecretValue123", "webhookSecretValue456");
    }

    @Test
    void registerOrUpdate_newProvider_createsCredential() {
        when(pgCredentialRepository.findByProvider("PORTONE")).thenReturn(Optional.empty());
        when(pgCredentialRepository.save(any(PgCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PgCredentialResponse response = pgCredentialService.registerOrUpdate(request());

        verify(pgCredentialRepository).save(any(PgCredential.class));
        assertThat(response.provider()).isEqualTo("PORTONE");
        assertThat(response.storeId()).isEqualTo("store-abc123");
        assertThat(response.channelKey()).isEqualTo("channel-key-toss-general");
    }

    @Test
    void registerOrUpdate_existingProvider_updatesInPlaceWithoutCreatingNew() {
        PgCredential existing = PgCredential.builder()
                .provider("PORTONE")
                .storeId("store-old")
                .channelKey("channel-old")
                .apiSecret(encryptor.encrypt("oldApiSecret"))
                .webhookSecret(encryptor.encrypt("oldWebhookSecret"))
                .build();
        when(pgCredentialRepository.findByProvider("PORTONE")).thenReturn(Optional.of(existing));

        pgCredentialService.registerOrUpdate(request());

        verify(pgCredentialRepository, never()).save(any());
        assertThat(existing.getStoreId()).isEqualTo("store-abc123");
        assertThat(existing.getChannelKey()).isEqualTo("channel-key-toss-general");
        assertThat(encryptor.decrypt(existing.getApiSecret())).isEqualTo("apiSecretValue123");
        assertThat(encryptor.decrypt(existing.getWebhookSecret())).isEqualTo("webhookSecretValue456");
    }

    @Test
    void registerOrUpdate_encryptsBothSecretsBeforeStoring() {
        when(pgCredentialRepository.findByProvider("PORTONE")).thenReturn(Optional.empty());
        when(pgCredentialRepository.save(any(PgCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<PgCredential> captor = ArgumentCaptor.forClass(PgCredential.class);

        pgCredentialService.registerOrUpdate(request());

        verify(pgCredentialRepository).save(captor.capture());
        PgCredential saved = captor.getValue();
        assertThat(saved.getApiSecret()).isNotEqualTo("apiSecretValue123");
        assertThat(saved.getWebhookSecret()).isNotEqualTo("webhookSecretValue456");
        assertThat(encryptor.decrypt(saved.getApiSecret())).isEqualTo("apiSecretValue123");
        assertThat(encryptor.decrypt(saved.getWebhookSecret())).isEqualTo("webhookSecretValue456");
    }

    @Test
    void registerOrUpdate_masksBothSecretsInResponse_showingOnlyLast4Chars() {
        when(pgCredentialRepository.findByProvider("PORTONE")).thenReturn(Optional.empty());
        when(pgCredentialRepository.save(any(PgCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PgCredentialResponse response = pgCredentialService.registerOrUpdate(request());

        assertThat(response.apiSecret()).isEqualTo("*************e123");
        assertThat(response.apiSecret()).doesNotContain("apiSecretValue123");
        assertThat(response.webhookSecret()).isEqualTo("*****************e456");
        assertThat(response.webhookSecret()).doesNotContain("webhookSecretValue456");
    }
}
