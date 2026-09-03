package com.example.memberservice;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.member.dto.SignupRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberSignupLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 회원가입에_성공하면_참가자_권한으로_생성된다() throws Exception {
        SignupRequest request = new SignupRequest("jisun@example.com", "password1234", "지선");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jisun@example.com"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    @Test
    void 이미_가입된_이메일로_재가입하면_409로_거절된다() throws Exception {
        SignupRequest request = new SignupRequest("dup@example.com", "password1234", "중복");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_DUPLICATE_EMAIL"));
    }

    @Test
    void 가입한_계정으로_로그인하면_액세스_토큰과_리프레시_쿠키를_발급받는다() throws Exception {
        signup("login@example.com", "password1234", "로그인");

        LoginRequest login = new LoginRequest("login@example.com", "password1234");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_401과_WWW_Authenticate_헤더로_거절된다() throws Exception {
        signup("wrongpw@example.com", "password1234", "틀림");

        LoginRequest login = new LoginRequest("wrongpw@example.com", "wrong-password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_쿠키로_재발급하면_새로운_토큰_쌍을_받는다() throws Exception {
        signup("refresh@example.com", "password1234", "재발급");
        Cookie refreshCookie = loginAndGetRefreshCookie("refresh@example.com", "password1234");

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().value("refreshToken", not(refreshCookie.getValue())));
    }

    @Test
    void refresh_쿠키가_없으면_401로_거부된다() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_MISSING"));
    }

    @Test
    void 이미_사용된_refresh_쿠키를_재사용하면_401과_함께_전체_세션이_무효화된다() throws Exception {
        signup("reuse@example.com", "password1234", "재사용");
        Cookie oldRefreshCookie = loginAndGetRefreshCookie("reuse@example.com", "password1234");

        MvcResult firstRotate = mockMvc.perform(post("/api/auth/refresh").cookie(oldRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        Cookie newRefreshCookie = firstRotate.getResponse().getCookie("refreshToken");

        // 이미 폐기된 oldRefreshCookie 재사용 -> 탐지되어 전체 세션 무효화
        mockMvc.perform(post("/api/auth/refresh").cookie(oldRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));

        // 방금 정상 발급받은 newRefreshCookie까지 같이 무효화됐는지 확인
        mockMvc.perform(post("/api/auth/refresh").cookie(newRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃하면_리프레시_쿠키가_삭제되고_이후_해당_토큰으로_재발급할_수_없다() throws Exception {
        signup("logout@example.com", "password1234", "로그아웃");
        Cookie refreshCookie = loginAndGetRefreshCookie("logout@example.com", "password1234");

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().value("refreshToken", ""))
                .andExpect(cookie().maxAge("refreshToken", 0));

        // 로그아웃으로 폐기된 토큰이므로 재사용 탐지 로직에 걸려 전체 세션이 무효화된다
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));
    }

    @Test
    void 리프레시_쿠키_없이_로그아웃해도_200으로_응답한다() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 이미_로그아웃한_토큰으로_다시_로그아웃해도_에러없이_처리된다() throws Exception {
        signup("relogout@example.com", "password1234", "재로그아웃");
        Cookie refreshCookie = loginAndGetRefreshCookie("relogout@example.com", "password1234");

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void signup(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, name))))
                .andExpect(status().isCreated());
    }

    private Cookie loginAndGetRefreshCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("refreshToken");
    }
}
