package com.example.memberservice.member.verifier;

import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.exception.MemberErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NtsBusinessVerifierTest {

    private static final String BASE_URL = "https://api.odcloud.kr";
    // 포털이 주는 Encoding 키 형태(%2B, %2F, %3D). 다시 인코딩되면 URL이 달라져서 테스트가 실패한다.
    private static final String ENCODED_KEY = "abc%2Bdef%2Fghi%3D%3D";
    private static final String STATUS_URL = BASE_URL + "/api/nts-businessman/v1/status?serviceKey=" + ENCODED_KEY;

    private static final String BUSINESS_NO = "1112223334";

    private MockRestServiceServer server;
    private NtsBusinessVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new NtsBusinessVerifier(builder.build(), BASE_URL, ENCODED_KEY);
    }

    @Test
    void 계속사업자면_ACTIVE를_반환한다() {
        server.expect(requestTo(STATUS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.b_no[0]").value(BUSINESS_NO))
                .andRespond(withSuccess(statusBody("계속사업자", "01"), MediaType.APPLICATION_JSON));

        assertThat(verifier.checkStatus(BUSINESS_NO)).isEqualTo(BusinessStatus.ACTIVE);
        server.verify();
    }

    @Test
    void 휴업자면_SUSPENDED를_반환한다() {
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess(statusBody("휴업자", "02"), MediaType.APPLICATION_JSON));

        assertThat(verifier.checkStatus(BUSINESS_NO)).isEqualTo(BusinessStatus.SUSPENDED);
    }

    @Test
    void 폐업자면_CLOSED를_반환한다() {
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess(statusBody("폐업자", "03"), MediaType.APPLICATION_JSON));

        assertThat(verifier.checkStatus(BUSINESS_NO)).isEqualTo(BusinessStatus.CLOSED);
    }

    @Test
    void 국세청에_등록되지_않은_번호면_NOT_REGISTERED를_반환한다() {
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("""
                        {"request_cnt":1,"match_cnt":0,"status_code":"OK",
                         "data":[{"b_no":"1112223334","b_stt":"","b_stt_cd":"",
                                  "tax_type":"국세청에 등록되지 않은 사업자등록번호입니다.","tax_type_cd":""}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(verifier.checkStatus(BUSINESS_NO)).isEqualTo(BusinessStatus.NOT_REGISTERED);
    }

    @Test
    void 알_수_없는_상태_코드면_확인_불가로_처리한다() {
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess(statusBody("알수없음", "99"), MediaType.APPLICATION_JSON));

        assertUnavailable();
    }

    @Test
    void 서버가_5xx를_반환하면_확인_불가로_처리한다() {
        server.expect(requestTo(STATUS_URL)).andRespond(withServerError());

        assertUnavailable();
    }

    @Test
    void 인증키가_거부되어_4xx가_오면_확인_불가로_처리한다() {
        server.expect(requestTo(STATUS_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertUnavailable();
    }

    @Test
    void 연결_타임아웃이_나면_확인_불가로_처리한다() {
        server.expect(requestTo(STATUS_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertUnavailable();
    }

    @Test
    void 응답의_data가_비어있으면_확인_불가로_처리한다() {
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status_code\":\"OK\",\"data\":[]}", MediaType.APPLICATION_JSON));

        assertUnavailable();
    }

    @Test
    void 서비스키가_비어있으면_생성에_실패한다() {
        assertThatThrownBy(() -> new NtsBusinessVerifier(RestClient.create(), BASE_URL, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NTS_SERVICE_KEY");
    }

    private static String statusBody(String statusText, String statusCode) {
        return """
                {"request_cnt":1,"match_cnt":1,"status_code":"OK",
                 "data":[{"b_no":"1112223334","b_stt":"%s","b_stt_cd":"%s",
                          "tax_type":"부가가치세 일반과세자","tax_type_cd":"01","end_dt":"","utcc_yn":"N"}]}
                """.formatted(statusText, statusCode);
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> verifier.checkStatus(BUSINESS_NO))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.BUSINESS_VERIFY_UNAVAILABLE));
    }
}
