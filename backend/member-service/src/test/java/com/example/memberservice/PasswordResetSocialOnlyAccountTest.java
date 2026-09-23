package com.example.memberservice;

import com.example.memberservice.auth.passwordreset.dto.PasswordResetRequest;
import com.example.memberservice.auth.session.dto.SocialLoginRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetSocialOnlyAccountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 소셜_전용_계정으로_재설정을_요청하면_거절된다() throws Exception {
        String email = "social-only@test.com";
        // 소셜 로그인으로만 가입된 회원 - password가 null인 상태를 mock provider로 재현
        mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(email + ":소셜전용", AgeGroup.TWENTIES, Job.DEVELOPER, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_SOCIAL_ONLY_ACCOUNT"));
    }
}
