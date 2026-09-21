package com.example.reservationservice.pgcredential;

import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.service.PgCredentialService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PgCredentialEnvInitializerTest {

    private final PgCredentialService service = mock(PgCredentialService.class);
    private final PgCredentialEnvInitializer initializer = new PgCredentialEnvInitializer(service);

    private void setEnv(String storeId, String channelKey, String apiSecret, String webhookSecret) {
        ReflectionTestUtils.setField(initializer, "storeId", storeId);
        ReflectionTestUtils.setField(initializer, "channelKey", channelKey);
        ReflectionTestUtils.setField(initializer, "apiSecret", apiSecret);
        ReflectionTestUtils.setField(initializer, "webhookSecret", webhookSecret);
    }

    @Test
    void env가_모두_있으면_PORTONE으로_등록한다() {
        setEnv("store-1", "channel-1", "secret-1", "whsec-1");

        initializer.run(null);

        ArgumentCaptor<PgCredentialRequest> captor = ArgumentCaptor.forClass(PgCredentialRequest.class);
        verify(service).registerOrUpdate(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new PgCredentialRequest("PORTONE", "store-1", "channel-1", "secret-1", "whsec-1"));
    }

    @Test
    void 웹훅_시크릿이_비어도_등록한다() {
        setEnv("store-1", "channel-1", "secret-1", "");

        initializer.run(null);

        verify(service).registerOrUpdate(any());
    }

    @Test
    void env가_없으면_아무것도_하지_않는다() {
        setEnv("", "", "", "");

        initializer.run(null);

        verifyNoInteractions(service);
    }

    @Test
    void 일부만_있으면_등록하지_않는다() {
        setEnv("store-1", "", "secret-1", "whsec-1");

        initializer.run(null);

        verifyNoInteractions(service);
    }
}
