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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

// paymentId만 신뢰하고, 실제 결제 여부·금액은 항상 PortOne 서버에 재조회해서 검증한다
// (PortOne 공식 권장 패턴: 클라이언트나 웹훅 본문 자체는 신뢰하지 않음)
@Slf4j
@Component
@RequiredArgsConstructor
public class PortOnePaymentVerifier {

    private static final String PROVIDER = "PORTONE";

    private final PgCredentialRepository pgCredentialRepository;
    private final PgCredentialEncryptor encryptor;

    public VerifiedPayment verify(String paymentId, int expectedAmount) {
        try (PaymentClient client = newClient()) {
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

    // 전액 취소. amount를 안 넘기면 PortOne이 남은 금액 전체를 취소 처리한다.
    public void cancel(String paymentId, String reason) {
        cancel(paymentId, null, reason);
    }

    // 결제 검증은 통과했지만 그 이후 좌석 확정에 실패한 경우(정원 초과 등)나, 참가자가 직접
    // 취소해서 환불해야 하는 경우(취소 시점에 따른 부분 환불 포함)에 쓰는 보상 트랜잭션.
    // 실패해도 호출부의 원래 처리(정원 초과 에러, 취소 자체는 완료 등)를 가리면 안 되므로
    // 예외를 던지지 않고 로그만 남긴다.
    // ponytail: 여기서도 실패하면 로그만 남고 끝 — 재시도 큐나 운영 알림은 필요해지면 추가한다.
    public void cancel(String paymentId, Integer amount, String reason) {
        try {
            Long amountAsLong = amount == null ? null : amount.longValue();
            try (PaymentClient client = newClient()) {
                client.cancelPayment(paymentId, amountAsLong, null, null, reason, null, null, null, null, null, null).get();
            }
        } catch (BusinessException e) {
            // PG 미등록 등 — 이 경우도 호출부 처리를 막으면 안 되므로 로그만 남긴다.
            log.error("PortOne 결제 취소(자동 환불) 실패 - {}: paymentId={}, amount={}, reason={}", e.getMessage(), paymentId, amount, reason);
        } catch (ExecutionException e) {
            log.error("PortOne 결제 취소(자동 환불) 실패: paymentId={}, amount={}, reason={}", paymentId, amount, reason, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("PortOne 결제 취소(자동 환불) 중단됨: paymentId={}, amount={}, reason={}", paymentId, amount, reason, e);
        }
    }

    // storeId를 넘겨야 한다: API Secret이 고객사(merchant) 단위 키면 storeId 없이는 결제 조회·취소가
    // 어느 상점 대상인지 몰라 PAYMENT_NOT_FOUND가 난다(부캠 공용 키가 이 경우). 상점 단위 키면 넘겨도 무해하다.
    private PaymentClient newClient() {
        PgCredential credential = pgCredentialRepository.findByProvider(PROVIDER)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.PG_NOT_CONFIGURED));
        return new PaymentClient(encryptor.decrypt(credential.getApiSecret()), "https://api.portone.io", credential.getStoreId());
    }

    public record VerifiedPayment(String paymentMethod) {}
}
