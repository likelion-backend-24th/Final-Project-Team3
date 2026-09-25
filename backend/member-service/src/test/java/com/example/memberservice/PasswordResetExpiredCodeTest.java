package com.example.memberservice;

import com.example.memberservice.auth.passwordreset.dto.PasswordResetConfirm;
import com.example.memberservice.auth.passwordreset.entity.PasswordResetToken;
import com.example.memberservice.auth.passwordreset.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetExpiredCodeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void 만료된_코드로_확인하면_거절된다() throws Exception {
        String email = "expired@test.com";
        passwordResetTokenRepository.save(
                PasswordResetToken.issue(email, "irrelevant-hash", LocalDateTime.now().minusMinutes(1))
        );

        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm(email, "123456", "newPassword1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_CODE_EXPIRED"));
    }

    @Test
    void 발급된적_없는_이메일로_확인하면_거절된다() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirm("never-requested@test.com", "123456", "newPassword1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_CODE_INVALID"));
    }
}
