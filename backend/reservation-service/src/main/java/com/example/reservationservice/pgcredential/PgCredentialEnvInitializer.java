package com.example.reservationservice.pgcredential;

import com.example.reservationservice.pgcredential.dto.PgCredentialRequest;
import com.example.reservationservice.pgcredential.service.PgCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 환경변수로 PortOne 키가 주어지면 기동 시 DB(pg_credential)에 암호화해 등록한다.
 * 이후 결제·웹훅·공개설정 코드는 기존처럼 DB만 읽으므로 그대로 동작한다.
 * env 값이 있으면 재기동마다 어드민 화면에서 입력한 값을 덮어쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgCredentialEnvInitializer implements ApplicationRunner {

    private static final String PROVIDER = "PORTONE";

    private final PgCredentialService pgCredentialService;

    @Value("${PORTONE_STORE_ID:}")
    private String storeId;

    @Value("${PORTONE_CHANNEL_KEY:}")
    private String channelKey;

    @Value("${PORTONE_API_SECRET:}")
    private String apiSecret;

    // 웹훅 시크릿은 웹훅 URL을 콘솔에 등록해야 발급되므로 비어 있어도 등록한다(웹훅만 401로 실패, 결제 승인은 영향 없음)
    @Value("${PORTONE_WEBHOOK_SECRET:}")
    private String webhookSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (storeId.isBlank() && channelKey.isBlank() && apiSecret.isBlank()) {
            return;
        }
        if (storeId.isBlank() || channelKey.isBlank() || apiSecret.isBlank()) {
            log.warn("PORTONE_STORE_ID / PORTONE_CHANNEL_KEY / PORTONE_API_SECRET 중 일부만 설정되어 env 등록을 건너뜁니다.");
            return;
        }
        pgCredentialService.registerOrUpdate(new PgCredentialRequest(PROVIDER, storeId, channelKey, apiSecret, webhookSecret));
        log.info("환경변수로 PortOne PG 설정을 등록했습니다.");
    }
}
