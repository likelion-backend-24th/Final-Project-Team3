package com.example.memberservice;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.auth.dto.RefreshRequest;
import com.example.memberservice.member.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void 가입한_계정으로_로그인하면_토큰_쌍을_발급받는다() throws Exception {
        signup("login@example.com", "password1234", "로그인");

        LoginRequest login = new LoginRequest("login@example.com", "password1234");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
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
    void refresh_token으로_재발급하면_새로운_토큰_쌍을_받는다() throws Exception {
        signup("refresh@example.com", "password1234", "재발급");
        String refreshToken = loginAndGetRefreshToken("refresh@example.com", "password1234");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(not(refreshToken)));
    }

    @Test
    void 이미_사용된_refresh_token을_재사용하면_401과_함께_전체_세션이_무효화된다() throws Exception {
        signup("reuse@example.com", "password1234", "재사용");
        String oldRefreshToken = loginAndGetRefreshToken("reuse@example.com", "password1234");
        String rotateBody = objectMapper.writeValueAsString(new RefreshRequest(oldRefreshToken));

        MvcResult firstRotate = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rotateBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode firstResult = objectMapper.readTree(firstRotate.getResponse().getContentAsString());
        String newRefreshToken = firstResult.path("data").path("refreshToken").asText();

        // 이미 폐기된 oldRefreshToken 재사용 -> 탐지되어 전체 세션 무효화
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rotateBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));

        // 방금 정상 발급받은 newRefreshToken까지 같이 무효화됐는지 확인
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(newRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private void signup(String email, String password, String name) throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(email, password, name))))
                .andExpect(status().isCreated());
    }

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("refreshToken").asText();
    }
}
