package com.example.memberservice;

import com.example.memberservice.auth.session.dto.LoginRequest;
import com.example.memberservice.auth.passwordreset.dto.PasswordResetConfirm;
import com.example.memberservice.auth.passwordreset.dto.PasswordResetRequest;
import com.example.memberservice.auth.emailverification.entity.EmailVerification;
import com.example.memberservice.auth.emailverification.repository.EmailVerificationRepository;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void 코드를_발송하고_확인하면_비밀번호가_재설정되고_새_비밀번호로_로그인할_수_있다() throws Exception {
        String email = "reset-flow@test.com";
        signupWithPassword(email, "oldPassword1234", "재설정테스트");

        String code = requestResetAndCapture(email);

        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, code, "newPassword1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "oldPassword1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "newPassword1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void 가입되지_않은_이메일로_재설정을_요청하면_거절된다() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest("no-such-member@test.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 틀린_코드로_확인하면_거절된다() throws Exception {
        String email = "wrong-code@test.com";
        signupWithPassword(email, "password1234", "틀린코드");
        String code = requestResetAndCapture(email);
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, wrongCode, "newPassword1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_CODE_INVALID"));
    }

    @Test
    void 틀린_코드를_5번_시도하면_6번째부터는_맞는_코드도_거절된다() throws Exception {
        String email = "brute-force@test.com";
        signupWithPassword(email, "password1234", "브루트포스");
        String code = requestResetAndCapture(email);
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/password/reset-confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, wrongCode, "newPassword1234"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_CODE_INVALID"));
        }

        // 시도 횟수를 다 소진했으니, 이번엔 맞는 코드를 내도 거절되어야 한다
        // (attempts 증가가 각 실패 트랜잭션 롤백에도 살아남았는지 함께 검증됨)
        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, code, "newPassword1234"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_ATTEMPTS_EXCEEDED"));
    }

    @Test
    void 재발송하면_이전_코드는_무효화된다() throws Exception {
        String email = "resend@test.com";
        signupWithPassword(email, "password1234", "재발송");

        String firstCode = requestResetAndCapture(email);
        String secondCode = requestResetAndCapture(email);

        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, firstCode, "newPassword1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_CODE_INVALID"));

        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, secondCode, "newPassword1234"))))
                .andExpect(status().isOk());
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

        // 같은 이메일로 재발송하는 테스트도 있어서 atLeastOnce로 검증하고, 가장 최근에 캡처된 코드를 반환한다
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        then(emailSender).should(atLeastOnce()).sendPasswordResetCode(eq(email), codeCaptor.capture(), anyLong());
        return codeCaptor.getValue();
    }
}
