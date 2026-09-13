package com.example.memberservice;

import com.example.memberservice.auth.dto.SendCodeRequest;
import com.example.memberservice.auth.dto.VerifyCodeRequest;
import com.example.memberservice.auth.repository.EmailVerificationRepository;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.member.dto.OrganizerSignupRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
class OrganizerSignupTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void 유효한_사업자등록번호로_가입하면_201과_ORGANIZER_role이_반환된다() throws Exception {
        String email = "newco@example.com";
        verifyEmail(email);

        OrganizerSignupRequest request = new OrganizerSignupRequest(
                email, "password1234", "김주최", "새회사", "1112223334"
        );

        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.organizationName").value("새회사"))
                .andExpect(jsonPath("$.data.businessNo").value("1112223334"))
                .andExpect(jsonPath("$.data.role").value("ORGANIZER"));
    }

    @Test
    void 사업자등록번호_형식이_틀리면_400으로_거절된다() throws Exception {
        OrganizerSignupRequest request = new OrganizerSignupRequest(
                "badno@example.com", "password1234", "김형식", "형식오류회사", "123456789A"
        );

        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_INVALID_BUSINESS_NO"));
    }

    @Test
    void 이미_가입된_이메일이면_409로_거절된다() throws Exception {
        signup("dupemail@example.com", "password1234", "첫번째", "첫회사", "2223334445");

        OrganizerSignupRequest duplicate = new OrganizerSignupRequest(
                "dupemail@example.com", "password1234", "두번째", "둘째회사", "3334445556"
        );

        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEMBER_DUPLICATE_EMAIL"));
    }

    @Test
    void 이미_등록된_사업자등록번호면_409로_거절된다() throws Exception {
        signup("first@example.com", "password1234", "첫번째", "첫회사", "4445556667");

        OrganizerSignupRequest duplicate = new OrganizerSignupRequest(
                "second@example.com", "password1234", "두번째", "둘째회사", "4445556667"
        );

        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEMBER_DUPLICATE_BUSINESS_NO"));
    }

    @Test
    void 인증완료하지_않은_이메일로_주최자_회원가입하면_거절된다() throws Exception {
        OrganizerSignupRequest request = new OrganizerSignupRequest(
                "not-verified-org@example.com", "password1234", "미인증", "미인증회사", "5556667778"
        );

        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_NOT_VERIFIED"));
    }

    @Test
    void 인증완료된_이메일로_주최자_가입하면_성공하고_인증기록이_소진된다() throws Exception {
        String email = "org-signup-after-verify@example.com";
        signup(email, "password1234", "인증완료", "인증완료회사", "6667778889");

        assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(email)).isFalse();
    }

    private void signup(String email, String password, String name, String organizationName, String businessNo) throws Exception {
        verifyEmail(email);

        OrganizerSignupRequest request = new OrganizerSignupRequest(email, password, name, organizationName, businessNo);
        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
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
