package com.example.memberservice;

import com.example.memberservice.auth.dto.SendCodeRequest;
import com.example.memberservice.auth.dto.VerifyCodeRequest;
import com.example.memberservice.auth.repository.EmailVerificationRepository;
import com.example.memberservice.auth.service.EmailSender;
import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.dto.OrganizerSignupRequest;
import com.example.memberservice.member.exception.MemberErrorCode;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.member.verifier.BusinessStatus;
import com.example.memberservice.member.verifier.BusinessVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주최자 가입 시 국세청 상태조회(BusinessVerifier) 결과에 따른 응답을 검증한다.
 * 실제 국세청 API는 호출하지 않고 BusinessVerifier를 목으로 대체한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizerBusinessVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private BusinessVerifier businessVerifier;

    @Test
    void 계속사업자면_201로_가입된다() throws Exception {
        String email = "org-biz-ok@example.com";
        verifyEmail(email);
        given(businessVerifier.checkStatus(any())).willReturn(BusinessStatus.ACTIVE);

        signupRequest(email, "7778889990")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ORGANIZER"));

        then(businessVerifier).should().checkStatus("7778889990");
        assertThat(memberRepository.existsByBusinessNo("7778889990")).isTrue();
    }

    @Test
    void 국세청에_등록되지_않은_번호면_400으로_거절되고_계정이_생성되지_않는다() throws Exception {
        String email = "org-biz-notreg@example.com";
        verifyEmail(email);
        given(businessVerifier.checkStatus(any())).willReturn(BusinessStatus.NOT_REGISTERED);

        signupRequest(email, "8889990001")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_BUSINESS_NOT_REGISTERED"));

        assertThat(memberRepository.existsByBusinessNo("8889990001")).isFalse();
        // 번호를 고쳐 다시 시도할 수 있도록 이메일 인증 기록은 소진되지 않아야 한다
        assertThat(emailVerificationRepository.existsByEmailAndVerifiedTrue(email)).isTrue();
    }

    @Test
    void 휴업한_사업자면_400으로_거절된다() throws Exception {
        assertInactiveRejected(BusinessStatus.SUSPENDED, "org-biz-suspended@example.com", "8889990002");
    }

    @Test
    void 폐업한_사업자면_400으로_거절된다() throws Exception {
        assertInactiveRejected(BusinessStatus.CLOSED, "org-biz-closed@example.com", "8889990003");
    }

    @Test
    void 국세청_연동이_실패하면_503으로_거절된다() throws Exception {
        String email = "org-biz-down@example.com";
        verifyEmail(email);
        given(businessVerifier.checkStatus(any()))
                .willThrow(new BusinessException(MemberErrorCode.BUSINESS_VERIFY_UNAVAILABLE));

        signupRequest(email, "9990001112")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("MEMBER_BUSINESS_VERIFY_UNAVAILABLE"));

        assertThat(memberRepository.existsByBusinessNo("9990001112")).isFalse();
    }

    @Test
    void 사업자등록번호_형식이_틀리면_국세청을_호출하지_않는다() throws Exception {
        signupRequest("org-biz-badformat@example.com", "123456789A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEMBER_INVALID_BUSINESS_NO"));

        then(businessVerifier).shouldHaveNoInteractions();
    }

    private void assertInactiveRejected(BusinessStatus inactive, String email, String businessNo) throws Exception {
        verifyEmail(email);
        given(businessVerifier.checkStatus(any())).willReturn(inactive);

        signupRequest(email, businessNo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEMBER_BUSINESS_NOT_ACTIVE"));

        assertThat(memberRepository.existsByBusinessNo(businessNo)).isFalse();
    }

    private ResultActions signupRequest(String email, String businessNo) throws Exception {
        OrganizerSignupRequest request = new OrganizerSignupRequest(
                email, "password1234", "김담당", "검증테스트회사", businessNo
        );
        return mockMvc.perform(post("/api/members/organizers/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
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
