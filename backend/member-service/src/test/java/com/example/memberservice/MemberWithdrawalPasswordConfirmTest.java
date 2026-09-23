package com.example.memberservice;

import com.example.memberservice.auth.session.dto.LoginRequest;
import com.example.memberservice.auth.emailverification.entity.EmailVerification;
import com.example.memberservice.auth.emailverification.repository.EmailVerificationRepository;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import com.example.memberservice.member.service.WithdrawRequest;
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
class MemberWithdrawalPasswordConfirmTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void 비밀번호_확인_후_탈퇴하면_이전_이메일로_재로그인할_수_없다() throws Exception {
        String email = "withdraw-pw@test.com";
        String password = "password1234";
        String accessToken = signupAndLogin(email, password, "탈퇴희망자");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest(password, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void 틀린_비밀번호로_탈퇴를_시도하면_거절된다() throws Exception {
        String email = "withdraw-wrong-pw@test.com";
        String accessToken = signupAndLogin(email, "password1234", "틀린비번");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("wrongPassword1", null, null, null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MEMBER_WITHDRAWAL_PASSWORD_MISMATCH"));
    }

    @Test
    void 인증_없이_탈퇴를_요청하면_거절된다() throws Exception {
        mockMvc.perform(delete("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("password1234", null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    private void markEmailVerified(String email) {
        EmailVerification verification = EmailVerification.issue(email, "test-hash", LocalDateTime.now().plusMinutes(10));
        verification.markVerified();
        emailVerificationRepository.save(verification);
    }

    private String signupAndLogin(String email, String password, String name) throws Exception {
        markEmailVerified(email);
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, name, AgeGroup.TWENTIES, Job.DEVELOPER))))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        String body = login.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("accessToken").asText();
    }
}
