package com.example.memberservice.member.service;

import org.springframework.stereotype.Component;

// 사업자등록번호 형식 검증
// 지금은 10자리 숫자만 확인. 추후 국세청 사업자등록정보 진위확인 API로 교체
@Component
public class BusinessNoValidator {

    private static final int BUSINESS_NO_LENGTH = 10;

    public boolean isValidFormat(String businessNo) {
        if (businessNo == null || businessNo.length() != BUSINESS_NO_LENGTH) {
            return false;
        }
        return businessNo.chars().allMatch(Character::isDigit);
    }
}
