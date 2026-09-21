package com.example.memberservice.member.verifier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 운영 기본값(real)에서 실제 구현체가 빈으로 주입되는지 확인한다. 국세청 API는 호출하지 않는다.
@SpringBootTest(properties = {
        "nts.verify.mode=real",
        "nts.verify.base-url=https://api.odcloud.kr",
        "nts.verify.service-key=dummy-key"
})
class NtsBusinessVerifierWiringTest {

    @Autowired
    private BusinessVerifier businessVerifier;

    @Test
    void real_모드에서는_국세청_구현체가_주입된다() {
        assertThat(businessVerifier).isInstanceOf(NtsBusinessVerifier.class);
    }
}
