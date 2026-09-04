package com.example.conferenceservice.auth;

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

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_SCHEME = "Bearer";

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = extractToken(header);

        if (token != null) {
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(String header) {
        if (header == null) {
            return null;
        }

        int spaceIndex = header.indexOf(' ');
        if (spaceIndex <= 0 || !BEARER_SCHEME.equalsIgnoreCase(header.substring(0, spaceIndex))) {
            return null;
        }

        return header.substring(spaceIndex + 1).trim();
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtTokenValidator.validate(token);
            UUID memberId = UUID.fromString(claims.getSubject());
            MemberRole role = MemberRole.valueOf(claims.get("role", String.class));
            CustomUserDetails userDetails = new CustomUserDetails(memberId, role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}
