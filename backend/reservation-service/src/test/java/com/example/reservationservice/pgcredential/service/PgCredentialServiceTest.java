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

    @Test
    void registerOrUpdate_newProvider_createsCredential() {
        when(pgCredentialRepository.findByProvider("TOSS")).thenReturn(Optional.empty());
        when(pgCredentialRepository.save(any(PgCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PgCredentialResponse response = pgCredentialService.registerOrUpdate(
                new PgCredentialRequest("TOSS", "api-key-abc", "supersecret1234"));

        verify(pgCredentialRepository).save(any(PgCredential.class));
        assertThat(response.provider()).isEqualTo("TOSS");
    }

    @Test
    void registerOrUpdate_existingProvider_updatesInPlaceWithoutCreatingNew() {
        PgCredential existing = PgCredential.builder()
                .provider("TOSS")
                .apiKey("old-api-key")
                .secretKey(encryptor.encrypt("oldsecret1234"))
                .build();
        when(pgCredentialRepository.findByProvider("TOSS")).thenReturn(Optional.of(existing));

        pgCredentialService.registerOrUpdate(new PgCredentialRequest("TOSS", "new-api-key", "newsecret5678"));

        verify(pgCredentialRepository, never()).save(any());
        assertThat(existing.getApiKey()).isEqualTo("new-api-key");
        assertThat(encryptor.decrypt(existing.getSecretKey())).isEqualTo("newsecret5678");
    }

    @Test
    void registerOrUpdate_encryptsSecretKeyBeforeStoring() {
        when(pgCredentialRepository.findByProvider("TOSS")).thenReturn(Optional.empty());
        when(pgCredentialRepository.save(any(PgCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<PgCredential> captor = ArgumentCaptor.forClass(PgCredential.class);

        pgCredentialService.registerOrUpdate(new PgCredentialRequest("TOSS", "api-key-abc", "mySecretKey123"));

        verify(pgCredentialRepository).save(captor.capture());
        String storedSecretKey = captor.getValue().getSecretKey();
        assertThat(storedSecretKey).isNotEqualTo("mySecretKey123");
        assertThat(encryptor.decrypt(storedSecretKey)).isEqualTo("mySecretKey123");
    }

    @Test
    void registerOrUpdate_masksSecretKeyInResponse_showingOnlyLast4Chars() {
        when(pgCredentialRepository.findByProvider("TOSS")).thenReturn(Optional.empty());
        when(pgCredentialRepository.save(any(PgCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PgCredentialResponse response = pgCredentialService.registerOrUpdate(
                new PgCredentialRequest("TOSS", "api-key-abc", "mySecretKey123"));

        assertThat(response.secretKey()).isEqualTo("**********y123");
        assertThat(response.secretKey()).doesNotContain("mySecretKey123");
    }
}
