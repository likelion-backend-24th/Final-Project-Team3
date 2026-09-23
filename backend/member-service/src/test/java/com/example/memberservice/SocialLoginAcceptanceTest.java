package com.example.memberservice;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.SocialLinkRequest;
import com.example.memberservice.auth.dto.SocialLoginRequest;
import com.example.memberservice.auth.entity.EmailVerification;
import com.example.memberservice.auth.repository.EmailVerificationRepository;
import com.example.memberservice.member.dto.OrganizerSignupRequest;
import com.example.memberservice.member.dto.SignupRequest;
import com.example.memberservice.member.entity.AgeGroup;
import com.example.memberservice.member.entity.Job;
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
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SocialLoginAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    void 소셜_최초_가입시_연령대_직무가_없으면_400으로_거절된다() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest("new1@test.com:테스트유저", null, null, null);

        mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_PROFILE_REQUIRED"));
    }

    @Test
    void 소셜_최초_가입은_연령대_직무와_함께_성공하고_MEMBER_권한으로_발급된다() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest("new2@test.com:테스트유저", AgeGroup.TWENTIES, Job.DEVELOPER, null);

        MvcResult result = mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = readAccessToken(result);
        assertThat(extractClaim(accessToken, "role")).isEqualTo("MEMBER");
        // ORGANIZER를 발급할 입력 자체가 API에 없다 — 이 role 값이 그 증거
    }

    @Test
    void 같은_소셜_계정으로_재로그인하면_새_계정이_생기지_않고_같은_회원으로_로그인된다() throws Exception {
        String token = "repeat@test.com:테스트유저";

        MvcResult first = mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLoginRequest(token, AgeGroup.TWENTIES, Job.DEVELOPER, null))))
                .andExpect(status().isOk())
                .andReturn();

        // 재로그인은 ageGroup/job 없이도 통과해야 한다(신규가 아니므로)
        MvcResult second = mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLoginRequest(token, null, null, null))))
                .andExpect(status().isOk())
                .andReturn();

        String firstMemberId = extractClaim(readAccessToken(first), "sub");
        String secondMemberId = extractClaim(readAccessToken(second), "sub");
        assertThat(secondMemberId).isEqualTo(firstMemberId);
    }

    @Test
    void 이미_비밀번호로_가입된_이메일로_소셜_로그인하면_자동연동되지_않고_409로_거절된다() throws Exception {
        signupWithPassword("collide@test.com", "password1234", "충돌");

        SocialLoginRequest request = new SocialLoginRequest("collide@test.com:소셜이름", AgeGroup.TWENTIES, Job.DEVELOPER, null);
        mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void 주최자로_가입된_이메일로_소셜_로그인하면_연동_안내_대신_전용_메시지로_거절된다() throws Exception {
        markEmailVerified("organizer-collide@test.com");
        OrganizerSignupRequest organizerRequest = new OrganizerSignupRequest(
                "organizer-collide@test.com", "password1234", "주최자", "테스트기관", "9998887770"
        );
        mockMvc.perform(post("/api/members/organizers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(organizerRequest)))
                .andExpect(status().isCreated());

        SocialLoginRequest request = new SocialLoginRequest("organizer-collide@test.com:소셜이름", AgeGroup.TWENTIES, Job.DEVELOPER, null);
        mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_EMAIL_NOT_LINKABLE"));
    }

    @Test
    void 지원하지_않는_provider로_요청하면_400으로_거절된다() throws Exception {
        SocialLoginRequest request = new SocialLoginRequest("x@test.com:x", AgeGroup.TWENTIES, Job.DEVELOPER, null);
        mockMvc.perform(post("/api/auth/social/naver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_PROVIDER_UNSUPPORTED"));
    }

    @Test
    void 로그인한_계정에_소셜_계정을_연동하면_이후_소셜_로그인으로_같은_계정에_들어온다() throws Exception {
        String email = "link@test.com";
        String accessToken = signupWithPassword(email, "password1234", "연동유저");
        String socialToken = email + ":연동소셜이름";

        mockMvc.perform(post("/api/auth/social/google/link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLinkRequest(socialToken, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult socialLoginResult = mockMvc.perform(post("/api/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLoginRequest(socialToken, null, null, null))))
                .andExpect(status().isOk())
                .andReturn();

        String passwordAccountMemberId = extractClaim(accessToken, "sub");
        String socialLoginMemberId = extractClaim(readAccessToken(socialLoginResult), "sub");
        assertThat(socialLoginMemberId).isEqualTo(passwordAccountMemberId);
    }

    @Test
    void 본인_이메일과_다른_소셜_계정을_연동하려_하면_400으로_거절된다() throws Exception {
        String accessToken = signupWithPassword("owner@test.com", "password1234", "본인");

        mockMvc.perform(post("/api/auth/social/google/link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLinkRequest("stranger@test.com:다른사람", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_LINK_EMAIL_MISMATCH"));
    }

    @Test
    void 이미_연동된_소셜_계정을_다시_연동하려_하면_409로_거절된다() throws Exception {
        String email = "double-link@test.com";
        String accessToken = signupWithPassword(email, "password1234", "중복연동");
        SocialLinkRequest linkRequest = new SocialLinkRequest(email + ":연동소셜이름", null);

        mockMvc.perform(post("/api/auth/social/google/link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/social/google/link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED"));
    }

    @Test
    void 연동_목록_조회는_연동된_provider를_반환한다() throws Exception {
        String email = "list-link@test.com";
        String accessToken = signupWithPassword(email, "password1234", "목록조회");

        mockMvc.perform(post("/api/auth/social/google/link")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLinkRequest(email + ":연동", null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/members/me/social-accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].provider").value("GOOGLE"))
                .andExpect(jsonPath("$.data[0].linkedAt").isNotEmpty());
    }

    @Test
    void 연동_계정이_없으면_빈_배열을_반환한다() throws Exception {
        String accessToken = signupWithPassword("no-link@test.com", "password1234", "미연동");

        mockMvc.perform(get("/api/members/me/social-accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 연동_목록_조회는_인증_없이_호출하면_거절된다() throws Exception {
        mockMvc.perform(get("/api/members/me/social-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 연동_엔드포인트는_인증_없이_호출하면_거절된다() throws Exception {
        mockMvc.perform(post("/api/auth/social/google/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLinkRequest("anyone@test.com:이름", null))))
                .andExpect(status().isUnauthorized());
    }

    private void markEmailVerified(String email) {
        EmailVerification verification = EmailVerification.issue(email, "test-hash", LocalDateTime.now().plusMinutes(10));
        verification.markVerified();
        emailVerificationRepository.save(verification);
    }

    // 비밀번호 계정 가입 + 로그인까지 한 번에 하고 accessToken을 돌려준다.
    private String signupWithPassword(String email, String password, String name) throws Exception {
        markEmailVerified(email);
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, name, AgeGroup.TWENTIES, Job.DEVELOPER))))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return readAccessToken(login);
    }

    private String readAccessToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("accessToken").asText();
    }

    // 서명 검증은 안 하고 payload만 읽는다 — 프론트 decodeJwt()와 같은 용도, 테스트에선 role/sub 확인용
    private String extractClaim(String jwt, String claim) throws Exception {
        String payload = jwt.split("\\.")[1];
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get(claim).asText();
    }
}
