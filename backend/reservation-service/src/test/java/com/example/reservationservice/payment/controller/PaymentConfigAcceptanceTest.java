package com.example.reservationservice.payment.controller;

import com.example.reservationservice.auth.CustomUserDetails;
import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentConfigAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PgCredentialRepository pgCredentialRepository;
    @Autowired
    private PgCredentialEncryptor pgCredentialEncryptor;

    @AfterEach
    void tearDown() {
        pgCredentialRepository.deleteAll();
    }

    private RequestPostProcessor asUser() {
        CustomUserDetails userDetails = new CustomUserDetails(UUID.randomUUID(), MemberRole.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        return authentication(auth);
    }

    @Test
    @DisplayName("로그인한 회원은 PG 공개 설정을 조회할 수 있고, 응답에 시크릿 필드가 아예 없다")
    void getPgConfig_returnsPublicFieldsOnly() throws Exception {
        pgCredentialRepository.save(PgCredential.builder()
                .provider("PORTONE")
                .storeId("store-abc123")
                .channelKey("channel-key-toss-general")
                .apiSecret(pgCredentialEncryptor.encrypt("apiSecretValue123"))
                .webhookSecret(pgCredentialEncryptor.encrypt("webhookSecretValue456"))
                .build());

        mockMvc.perform(get("/api/payments/pg-config").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("PORTONE"))
                .andExpect(jsonPath("$.data.storeId").value("store-abc123"))
                .andExpect(jsonPath("$.data.channelKey").value("channel-key-toss-general"))
                .andExpect(jsonPath("$.data.apiSecret").doesNotExist())
                .andExpect(jsonPath("$.data.webhookSecret").doesNotExist());
    }

    @Test
    @DisplayName("등록된 PG 설정이 없으면 404를 반환한다")
    void getPgConfig_notConfigured_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/pg-config").with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESERVATION_PG_CONFIG_NOT_FOUND"));
    }

    @Test
    @DisplayName("비로그인 요청은 401로 거부된다")
    void getPgConfig_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/pg-config"))
                .andExpect(status().isUnauthorized());
    }
}
