package com.example.memberservice.common;

public final class EmailUtil {

    private EmailUtil() {
    }

    // 이메일은 앞뒤 공백을 빼고 소문자로 맞춰 저장·조회한다. (User@Example.com 과 user@example.com 을 같은 계정으로 취급)
    public static String normalize(String email) {
        return email.strip().toLowerCase();
    }
}
