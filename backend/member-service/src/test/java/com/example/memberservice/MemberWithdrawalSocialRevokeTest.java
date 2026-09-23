package com.example.memberservice;

import com.example.memberservice.auth.session.dto.SocialLoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberWithdrawalSocialRevokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 소셜_전용_계정은_소셜_재인증_후_탈퇴된다() throws Exception {
        String socialToken = "social-withdraw@test.com:소셜탈퇴";
        String accessToken = socialSignup(socialToken);

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest(null, "google", socialToken, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 연동되지_않은_다른_사람의_소셜_계정으로_탈퇴를_시도하면_거절된다() throws Exception {
        String accessToken = socialSignup("owner@test.com:소유자");
        // 다른 사람(stranger)의 소셜 계정으로 재인증을 시도 — 토큰 자체는 유효하지만 owner 계정에 연동된 게 아님
        socialSignup("stranger@test.com:다른사람");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest(null, "google", "stranger@test.com:다른사람", null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MEMBER_WITHDRAWAL_SOCIAL_IDENTITY_MISMATCH"));
    }

    @Test
    void 소셜_전용_계정이_재인증_정보_없이_탈퇴를_시도하면_거절된다() throws Exception {
        String accessToken = socialSignup("no-reauth@test.com:재인증누락");

        mockMvc.perform(delete("/api/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WithdrawRequest(null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_PROVIDER_UNSUPPORTED"));
    }

    private String socialSignup(String socialToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLoginRequest(socialToken, AgeGroup.TWENTIES, Job.DEVELOPER, null))))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("accessToken").asText();
    }
}
