package com.example.memberservice.member.service;

import org.springframework.stereotype.Component;

// 사업자등록번호 형식 검증(10자리 숫자). 국세청 상태조회 API를 호출하기 전에 잘못된 입력을 싸게 걸러내는 용도.
// 실제 존재·영업 여부는 BusinessVerifier가 확인한다.
@Component
public class BusinessNoValidator {

    private static final int BUSINESS_NO_LENGTH = 10;

    public boolean isValidFormat(String businessNo) {
        if (businessNo == null || businessNo.length() != BUSINESS_NO_LENGTH) {
            return false;
        }
        return businessNo.chars().allMatch(c -> c >= '0' && c <= '9');
    }
}
