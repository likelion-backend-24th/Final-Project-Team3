package com.example.memberservice.auth.session.security;

import com.example.memberservice.member.entity.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// 매 요청마다 실행되는 검문소
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_SCHEME = "Bearer";

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = extractToken(header);

        if (token != null) {
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    // Bearer 제거
    private String extractToken(String header) {
        if (header == null) return null;

        int spaceIndex = header.indexOf(' ');
        if (spaceIndex <= 0 || !BEARER_SCHEME.equalsIgnoreCase(header.substring(0, spaceIndex))) return null;

        return header.substring(spaceIndex + 1).trim();
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtTokenValidator.validate(token);
            //JwtTokenProvider가 토큰 발급할 때 subject=memberId, claim("role")=역할로 넣어뒀던 걸 그대로 꺼내는 것
            UUID memberId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            CustomUserDetails userDetails = new CustomUserDetails(memberId, role);

            // 이 사람이 인증됐다고 Spring Security에 등록
            // 여기 등록된 값을 컨트롤러가 @AuthenticationPrincipal로 꺼내 쓰게 됨
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException e) {
            // 토큰이 위조/만료됐으면 그냥 인증 안 된 상태로 흘려보냄
            // 애초에 인증이 필요 없는 API일 수도 있기 때문에 여기서 바로 401을 내리지 않음
            log.debug("Failed to validate JWT token", e);
            SecurityContextHolder.clearContext();
        }
    }
}
