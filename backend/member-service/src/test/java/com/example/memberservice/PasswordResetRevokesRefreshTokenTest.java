package com.example.memberservice;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.PasswordResetConfirm;
import com.example.memberservice.auth.dto.PasswordResetRequest;
import com.example.memberservice.auth.entity.EmailVerification;
import com.example.memberservice.auth.repository.EmailVerificationRepository;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetRevokesRefreshTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void 재설정에_성공하면_재설정_이전에_발급된_리프레시_토큰은_재사용_탐지로_거절된다() throws Exception {
        String email = "revoke-refresh@test.com";
        String oldPassword = "oldPassword1234";
        signupWithPassword(email, oldPassword, "리프레시무효화");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, oldPassword))))
                .andExpect(status().isOk())
                .andReturn();
        String oldRefreshToken = loginResult.getResponse().getCookie("refreshToken").getValue();

        String code = requestResetAndCapture(email);
        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, code, "newPassword1234"))))
                .andExpect(status().isOk());

        // 재설정 성공 시 기존 계정의 모든 Refresh Token이 revoked 처리되므로,
        // 재설정 이전에 발급된 토큰으로 재발급을 시도하면 "이미 폐기된 토큰 재사용" 경로로 거절되어야 한다
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

    private void signupWithPassword(String email, String password, String name) throws Exception {
        markEmailVerified(email);
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, name, AgeGroup.TWENTIES, Job.DEVELOPER))))
                .andExpect(status().isCreated());
    }

    private String requestResetAndCapture(String email) throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest(email))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        then(emailSender).should().sendPasswordResetCode(eq(email), codeCaptor.capture(), anyLong());
        return codeCaptor.getValue();
    }
}
