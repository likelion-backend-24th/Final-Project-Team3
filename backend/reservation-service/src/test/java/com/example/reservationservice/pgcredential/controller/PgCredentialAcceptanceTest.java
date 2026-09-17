package com.example.reservationservice.pgcredential.controller;

import com.example.reservationservice.auth.MemberRole;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PgCredentialAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PgCredentialRepository pgCredentialRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void tearDown() {
        pgCredentialRepository.deleteAll();
    }

    @Test
    void registerPgKey_asAdmin_savesAndMasksResponse() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/pg-key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "PORTONE",
                                  "storeId": "store-abc123",
                                  "channelKey": "channel-key-toss-general",
                                  "apiSecret": "apiSecretValue123",
                                  "webhookSecret": "webhookSecretValue456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("PORTONE"))
                .andExpect(jsonPath("$.data.storeId").value("store-abc123"))
                .andExpect(jsonPath("$.data.channelKey").value("channel-key-toss-general"))
                .andExpect(jsonPath("$.data.apiSecret").value("*************e123"))
                .andExpect(jsonPath("$.data.webhookSecret").value("*****************e456"));
    }

    @Test
    void registerPgKey_withoutAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/pg-key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "PORTONE",
                                  "storeId": "store-abc123",
                                  "channelKey": "channel-key-toss-general",
                                  "apiSecret": "apiSecretValue123",
                                  "webhookSecret": "webhookSecretValue456"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerPgKey_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/pg-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "PORTONE",
                                  "storeId": "store-abc123",
                                  "channelKey": "channel-key-toss-general",
                                  "apiSecret": "apiSecretValue123",
                                  "webhookSecret": "webhookSecretValue456"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerPgKey_missingRequiredField_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/pg-key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "PORTONE",
                                  "storeId": "store-abc123",
                                  "channelKey": "channel-key-toss-general"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String adminToken() {
        return token(MemberRole.ADMIN);
    }

    private String organizerToken() {
        return token(MemberRole.ORGANIZER);
    }

    private String token(MemberRole role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
