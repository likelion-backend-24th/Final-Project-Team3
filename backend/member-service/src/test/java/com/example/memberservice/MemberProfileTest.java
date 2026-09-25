package com.example.memberservice;

import com.example.memberservice.auth.session.dto.LoginRequest;
import com.example.memberservice.auth.emailverification.dto.SendCodeRequest;
import com.example.memberservice.auth.emailverification.dto.VerifyCodeRequest;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.dto.UpdateProfileRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Task 2-6: 마이페이지 프로필(연령대·직무) 수정 Acceptance Test.
// PATCH /api/members/me는 JwtAuthenticationFilter를 거쳐야 하는 첫 "로그인한 내가 나를 조회/수정" API라,
// 인증 인프라(필터·SecurityConfig)가 제대로 동작하는지까지 같이 검증하는 셈이다.
@SpringBootTest
@AutoConfigureMockMvc
class MemberProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void 인증_없이_호출하면_401로_거절된다() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(AgeGroup.THIRTIES, Job.DESIGNER);

        mockMvc.perform(patch("/api/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void 유효하지_않은_토큰으로_호출하면_401로_거절된다() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(AgeGroup.THIRTIES, Job.DESIGNER);

        mockMvc.perform(patch("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this-is-not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void 조회도_인증_없이_호출하면_401로_거절된다() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void 유효한_토큰으로_조회하면_가입때_입력한_연령대_직무가_그대로_반환된다() throws Exception {
        String email = "profile-view@example.com";
        String accessToken = signupAndLogin(email, "password1234", "프로필조회", AgeGroup.THIRTIES, Job.DATA_AI);

        mockMvc.perform(get("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.ageGroup").value("THIRTIES"))
                .andExpect(jsonPath("$.data.job").value("DATA_AI"));
    }

    @Test
    void 연령대나_직무가_없으면_400으로_거절된다() throws Exception {
        String accessToken = signupAndLogin("profile-invalid@example.com", "password1234", "프로필무효", AgeGroup.TWENTIES, Job.DEVELOPER);

        mockMvc.perform(patch("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ageGroup\":\"THIRTIES\"}")) // job 누락
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 유효한_토큰으로_프로필을_수정하면_응답과_DB에_반영된다() throws Exception {
        String email = "profile-update@example.com";
        String accessToken = signupAndLogin(email, "password1234", "프로필수정", AgeGroup.TWENTIES, Job.DEVELOPER);

        UpdateProfileRequest request = new UpdateProfileRequest(AgeGroup.FORTIES, Job.MARKETING_SALES);

        mockMvc.perform(patch("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ageGroup").value("FORTIES"))
                .andExpect(jsonPath("$.data.job").value("MARKETING_SALES"));

        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getAgeGroup()).isEqualTo(AgeGroup.FORTIES);
        assertThat(member.getJob()).isEqualTo(Job.MARKETING_SALES);
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
