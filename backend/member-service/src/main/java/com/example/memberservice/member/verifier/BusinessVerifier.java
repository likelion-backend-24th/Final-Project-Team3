package com.example.memberservice.member.verifier;

public interface BusinessVerifier {

    /**
     * 국세청에 조회한 사업자등록번호의 상태를 반환한다.
     * 외부 연동 자체가 실패하면 MEMBER_BUSINESS_VERIFY_UNAVAILABLE(BusinessException)을 던진다.
     */
    BusinessStatus checkStatus(String businessNo);
}
