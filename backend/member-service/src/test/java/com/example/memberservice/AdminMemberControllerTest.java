package com.example.memberservice;

import com.example.memberservice.auth.emailverification.dto.SendCodeRequest;
import com.example.memberservice.auth.emailverification.dto.VerifyCodeRequest;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.auth.session.dto.LoginRequest;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Story 18 Task 18-4: 회원 목록·상세 조회는 전체관리자만 가능하다 (PRD 규칙 [회원조회-관리자전용]).
@SpringBootTest
@AutoConfigureMockMvc
class AdminMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void 토큰_없이_회원_목록을_조회하면_401로_거절된다() throws Exception {
        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void 일반_회원_토큰으로_회원_목록을_조회하면_403으로_거절된다() throws Exception {
        String accessToken = signupAndLogin("admin-api-member@example.com", "password1234", "일반회원",
                AgeGroup.TWENTIES, Job.DEVELOPER);

        mockMvc.perform(get("/api/admin/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void 일반_회원_토큰으로_회원_상세를_조회하면_403으로_거절된다() throws Exception {
        String accessToken = signupAndLogin("admin-api-detail@example.com", "password1234", "일반회원상세",
                AgeGroup.TWENTIES, Job.DEVELOPER);

        mockMvc.perform(get("/api/admin/members/{memberId}", java.util.UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    private String signupAndLogin(String email, String password, String name, AgeGroup ageGroup, Job job) throws Exception {
        verifyEmail(email);

        SignupRequest signupRequest = new SignupRequest(email, password, name, ageGroup, job);
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    private void verifyEmail(String email) throws Exception {
        mockMvc.perform(post("/api/auth/email/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendCodeRequest(email))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        then(emailSender).should(atLeastOnce()).sendVerificationCode(eq(email), codeCaptor.capture(), anyLong());
        String code = codeCaptor.getValue();

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, code))))
                .andExpect(status().isOk());
    }
}