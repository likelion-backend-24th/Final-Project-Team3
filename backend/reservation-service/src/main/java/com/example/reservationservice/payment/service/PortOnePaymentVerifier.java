package com.example.reservationservice.payment.service;

import com.example.reservationservice.common.exception.BusinessException;
import com.example.reservationservice.pgcredential.PgCredentialEncryptor;
import com.example.reservationservice.pgcredential.entity.PgCredential;
import com.example.reservationservice.pgcredential.repository.PgCredentialRepository;
import com.example.reservationservice.reservation.exception.ReservationErrorCode;
import io.portone.sdk.server.errors.PaymentNotFoundException;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.Payment;
import io.portone.sdk.server.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

// paymentId만 신뢰하고, 실제 결제 여부·금액은 항상 PortOne 서버에 재조회해서 검증한다
// (PortOne 공식 권장 패턴: 클라이언트나 웹훅 본문 자체는 신뢰하지 않음)
@Component
@RequiredArgsConstructor
public class PortOnePaymentVerifier {

    private static final String PROVIDER = "PORTONE";

    private final PgCredentialRepository pgCredentialRepository;
    private final PgCredentialEncryptor encryptor;

    public VerifiedPayment verify(String paymentId, int expectedAmount) {
        String apiSecret = decryptApiSecret();

        try (PaymentClient client = new PaymentClient(apiSecret, "https://api.portone.io", null)) {
            Payment payment = client.getPayment(paymentId).get();

            if (!(payment instanceof PaidPayment paid)) {
                throw new BusinessException(ReservationErrorCode.PAYMENT_NOT_PAID);
            }
            if (paid.getAmount().getTotal() != expectedAmount) {
                throw new BusinessException(ReservationErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }
            // ponytail: 결제수단 클래스명을 그대로 저장 (PaymentMethodCard 등). 화면에 예쁘게 보여줄
            // 필요가 생기면 그때 타입별 한글 라벨 매핑을 추가한다.
            String paymentMethod = paid.getMethod() != null
                    ? paid.getMethod().getClass().getSimpleName()
                    : "UNKNOWN";
            return new VerifiedPayment(paymentMethod);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PaymentNotFoundException) {
                throw new BusinessException(ReservationErrorCode.PAYMENT_NOT_FOUND);
            }
            throw new BusinessException(ReservationErrorCode.PORTONE_API_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ReservationErrorCode.PORTONE_API_ERROR);
        }
    }

    private String decryptApiSecret() {
        PgCredential credential = pgCredentialRepository.findByProvider(PROVIDER)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.PG_NOT_CONFIGURED));
        return encryptor.decrypt(credential.getApiSecret());
    }

    public record VerifiedPayment(String paymentMethod) {}
}
