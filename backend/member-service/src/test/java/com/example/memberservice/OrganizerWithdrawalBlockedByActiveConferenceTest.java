package com.example.memberservice;

import com.example.memberservice.auth.session.dto.LoginRequest;
import com.example.memberservice.auth.emailverification.entity.EmailVerification;
import com.example.memberservice.auth.emailverification.repository.EmailVerificationRepository;
import com.example.memberservice.member.client.ConferenceServiceClient;
import com.example.memberservice.member.client.ConferenceServiceUnavailableException;
import com.example.memberservice.member.dto.OrganizerSignupRequest;
import com.example.memberservice.member.service.WithdrawRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizerWithdrawalBlockedByActiveConferenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    // 실제 Conference-Service를 띄우지 않고, 응답을 직접 통제해서 각 시나리오를 재현한다
    @MockitoBean
    private ConferenceServiceClient conferenceServiceClient;

    @Test
    void 진행중인_컨퍼런스가_있으면_주최자_탈퇴가_거절된다() throws Exception {
        given(conferenceServiceClient.hasActiveConference(any(UUID.class))).willReturn(true);
        String accessToken = signupOrganizerAndLogin("organizer-blocked@test.com", "6667778889", "차단주최자");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("password1234", null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEMBER_ORGANIZER_WITHDRAWAL_BLOCKED"));
    }

    @Test
    void 진행중인_컨퍼런스가_없으면_주최자도_탈퇴할_수_있다() throws Exception {
        given(conferenceServiceClient.hasActiveConference(any(UUID.class))).willReturn(false);
        String accessToken = signupOrganizerAndLogin("organizer-free@test.com", "7778889990", "탈퇴가능주최자");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("password1234", null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 컨퍼런스서비스_조회가_실패하면_주최자_탈퇴가_차단된다() throws Exception {
        given(conferenceServiceClient.hasActiveConference(any(UUID.class)))
                .willThrow(new ConferenceServiceUnavailableException(UUID.randomUUID(), new RuntimeException("timeout")));
        String accessToken = signupOrganizerAndLogin("organizer-unavailable@test.com", "8889990001", "장애주최자");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest("password1234", null, null, null))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("MEMBER_WITHDRAWAL_ELIGIBILITY_CHECK_FAILED"));
    }

    private void markEmailVerified(String email) {
        EmailVerification verification = EmailVerification.issue(email, "test-hash", LocalDateTime.now().plusMinutes(10));
        verification.markVerified();
        emailVerificationRepository.save(verification);
    }

    private String signupOrganizerAndLogin(String email, String businessNo, String name) throws Exception {
        markEmailVerified(email);
        String password = "password1234";
        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizerSignupRequest(email, password, name, "테스트기관", businessNo))))
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
