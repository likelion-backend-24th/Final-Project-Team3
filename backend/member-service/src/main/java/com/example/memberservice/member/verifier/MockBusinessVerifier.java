package com.example.memberservice.member.verifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 로컬·테스트 전용. nts.verify.mode=mock 일 때만 등록되며 항상 계속사업자로 응답한다.
@Slf4j
@Component
@ConditionalOnProperty(name = "nts.verify.mode", havingValue = "mock")
public class MockBusinessVerifier implements BusinessVerifier {

    @Override
    public BusinessStatus checkStatus(String businessNo) {
        log.warn("[MOCK] 국세청 상태조회를 건너뛰고 계속사업자로 처리합니다: {}", businessNo);
        return BusinessStatus.ACTIVE;
    }
}
