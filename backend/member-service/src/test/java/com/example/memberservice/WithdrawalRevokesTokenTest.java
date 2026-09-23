package com.example.memberservice;

import com.example.memberservice.auth.session.dto.LoginRequest;
import com.example.memberservice.auth.emailverification.entity.EmailVerification;
import com.example.memberservice.auth.emailverification.repository.EmailVerificationRepository;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import com.example.memberservice.member.service.WithdrawRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WithdrawalRevokesTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void 탈퇴하면_탈퇴_이전에_발급된_리프레시_토큰은_재사용_탐지로_거절된다() throws Exception {
        String email = "withdraw-revoke@test.com";
        String password = "password1234";
        markEmailVerified(email);
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, "토큰무효화", AgeGroup.TWENTIES, Job.DEVELOPER))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = readAccessToken(loginResult);
        String oldRefreshToken = loginResult.getResponse().getCookie("refreshToken").getValue();

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest(password, null, null, null))))
                .andExpect(status().isOk());

        // 탈퇴 성공 시 기존 계정의 모든 Refresh Token이 revoked 처리되므로,
        // 탈퇴 이전에 발급된 토큰으로 재발급을 시도하면 "이미 폐기된 토큰 재사용" 경로로 거절되어야 한다
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));
    }

    private void markEmailVerified(String email) {
        EmailVerification verification = EmailVerification.issue(email, "test-hash", LocalDateTime.now().plusMinutes(10));
        verification.markVerified();
        emailVerificationRepository.save(verification);
    }

    private String readAccessToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("accessToken").asText();
    }
}
