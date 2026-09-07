package com.example.memberservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private static final int BCRYPT_STRENGTH = 12;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 근거: 이 서비스는 세션 쿠키가 아니라 Access Token(Bearer, 요청 본문/헤더로 명시 전달)으로
                // 인증하므로 일반적인 세션 기반 CSRF는 해당하지 않는다. 유일하게 쿠키만으로 동작하는 refreshToken은
                // SameSite=Lax(application.yaml)로 발급되어 크로스사이트 POST 요청에는 브라우저가 쿠키를 붙이지 않는다.
                // 단, 배포 환경에서 프론트가 다른 도메인이라 COOKIE_SAMESITE=None으로 오버라이드하는 경우
                // 이 방어가 무력화되므로, 그때는 별도 CSRF 토큰 방어를 추가해야 한다.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/members/signup", "/api/members/organizers/signup", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
