package com.example.memberservice.auth.controller;

import com.example.memberservice.auth.dto.SendCodeRequest;
import com.example.memberservice.auth.dto.VerifyCodeRequest;
import com.example.memberservice.auth.entity.EmailVerification;
import com.example.memberservice.auth.repository.EmailVerificationRepository;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.member.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void 발송한_코드로_검증하면_인증이_완료된다() throws Exception {
        String email = "verify-ok@example.com";
        String code = sendCodeAndCapture(email);

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(email)).isTrue();
    }

    @Test
    void 발급된적_없는_이메일로_검증하면_거절된다() throws Exception {
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest("never-sent@example.com", "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_CODE_INVALID"));
    }

    @Test
    void 만료된_코드로_검증하면_거절된다() throws Exception {
        String email = "expired@example.com";
        emailVerificationRepository.save(
                EmailVerification.issue(email, "irrelevant-hash", LocalDateTime.now().minusMinutes(1))
        );

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_CODE_EXPIRED"));
    }

    @Test
    void 틀린_코드를_5번_시도하면_6번째부터는_맞는_코드도_거절된다() throws Exception {
        String email = "brute-force@example.com";
        String code = sendCodeAndCapture(email);
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, wrongCode))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_CODE_INVALID"));
        }

        // 시도 횟수를 다 소진했으니, 이번엔 맞는 코드를 내도 거절되어야 한다
        // (attempts 증가가 각 실패 트랜잭션 롤백에도 살아남았는지 함께 검증됨)
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, code))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_CODE_ATTEMPTS_EXCEEDED"));

        assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(email)).isFalse();
    }

    @Test
    void 재발송하면_이전_코드는_무효화된다() throws Exception {
        String email = "resend@example.com";
        String firstCode = sendCodeAndCapture(email);
        String secondCode = sendCodeAndCapture(email);

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, firstCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_CODE_INVALID"));

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, secondCode))))
                .andExpect(status().isOk());
    }

    @Test
    void 이미_가입된_이메일로_인증코드를_요청하면_거절된다() throws Exception {
        String email = "already-member@example.com";
        signupVerifiedMember(email, "password1234", "가입완료");

        mockMvc.perform(post("/api/auth/email/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendCodeRequest(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEMBER_DUPLICATE_EMAIL"));
    }

    @Test
    void 인증완료하지_않은_이메일로_회원가입하면_거절된다() throws Exception {
        SignupRequest request = new SignupRequest("not-verified@example.com", "password1234", "미인증");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_NOT_VERIFIED"));
    }

    @Test
    void 인증완료된_이메일로_회원가입하면_성공하고_인증기록이_소진된다() throws Exception {
        String email = "signup-after-verify@example.com";

        signupVerifiedMember(email, "password1234", "인증완료가입");

        assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(email)).isFalse();
    }

    private String sendCodeAndCapture(String email) throws Exception {
        mockMvc.perform(post("/api/auth/email/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendCodeRequest(email))))
                .andExpect(status().isOk());

        // 같은 이메일로 재발송하는 테스트도 있어서 atLeastOnce로 검증하고, 가장 최근에 캡처된 코드를 반환한다
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        then(emailSender).should(atLeastOnce()).sendVerificationCode(eq(email), codeCaptor.capture(), anyLong());
        return codeCaptor.getValue();
    }

    private void signupVerifiedMember(String email, String password, String name) throws Exception {
        String code = sendCodeAndCapture(email);

        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCodeRequest(email, code))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, name))))
                .andExpect(status().isCreated());
    }
}
