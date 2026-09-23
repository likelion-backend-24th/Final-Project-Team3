package com.example.memberservice.auth.session.security;

import com.example.memberservice.member.entity.Role;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// 인증된 사용자를 표현하는 그릇
// Spring Security는 요청을 보낸 사람의 정보를 SecurityContext에 저장하고, 컨트롤러가 나중에 꺼내씀
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID memberId;
    private final Role role;

    public CustomUserDetails(UUID memberId, Role role) {
        this.memberId = memberId;
        this.role = role;
    }

    // Spring Security의 권한 체계는 "ROLE_XXX" 문자열로 권한을 구분
    // 나중에 @PreAuthorize("hasRole('ORGANIZER')") 같은 걸 쓰려면 이 형식이 필요
    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
    }
}
