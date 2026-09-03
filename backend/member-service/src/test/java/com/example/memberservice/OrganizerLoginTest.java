package com.example.memberservice;

import com.example.memberservice.auth.dto.LoginRequest;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import com.example.memberservice.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 8-1 검증: Role=ORGANIZER 계정도 기존 로그인 API(Story 2)로 로그인할 수 있고,
 * 발급된 JWT에 organizerId claim이 담기는지 확인한다.
 *
 * Task 13-1(주최자 계정 생성 API)이 아직 없어 회원가입 API로는 ORGANIZER 계정을 만들 수 없다.
 * 그래서 여기서는 MEMBER로 만든 뒤 role만 ORGANIZER로 바꿔서 저장한다 (Member 엔티티는 그대로 둠).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrganizerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void ORGANIZER_계정도_로그인_API로_로그인할_수_있다() throws Exception {
        createOrganizer("organizer@example.com", "password1234", "주최자");

        LoginRequest login = new LoginRequest("organizer@example.com", "password1234");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void ORGANIZER로_로그인하면_JWT에_organizerId_claim이_담긴다() throws Exception {
        Member organizer = createOrganizer("scope@example.com", "password1234", "범위확인");

        String accessToken = loginAndGetAccessToken("scope@example.com", "password1234");
        Claims claims = parseClaims(accessToken);

        assertThat(claims.get("role", String.class)).isEqualTo("ORGANIZER");
        assertThat(claims.get("organizerId", String.class)).isEqualTo(organizer.getId().toString());
    }

    @Test
    void MEMBER로_로그인하면_JWT에_organizerId_claim이_없다() throws Exception {
        Member member = Member.newMember("member@example.com", passwordEncoder.encode("password1234"), "참가자");
        memberRepository.save(member);

        String accessToken = loginAndGetAccessToken("member@example.com", "password1234");
        Claims claims = parseClaims(accessToken);

        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(claims.get("organizerId")).isNull();
    }

    private Member createOrganizer(String email, String password, String name) {
        Member member = Member.newMember(email, passwordEncoder.encode(password), name);
        ReflectionTestUtils.setField(member, "role", Role.ORGANIZER);
        return memberRepository.save(member);
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
