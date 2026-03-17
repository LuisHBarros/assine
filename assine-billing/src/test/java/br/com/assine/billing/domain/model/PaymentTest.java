package br.com.assine.billing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void shouldCreatePendingPayment() {
        PaymentId id = new PaymentId(UUID.randomUUID());
        UUID subscriptionId = UUID.randomUUID();
        String externalId = "ext-123";
        IdempotencyKey key = new IdempotencyKey("idem-123");

        Payment payment = Payment.create(id, subscriptionId, externalId, key, 1000, PaymentMethod.CREDIT_CARD);

        assertThat(payment.getId()).isEqualTo(id);
        assertThat(payment.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getConfirmedAt()).isNull();
        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldConfirmPendingPayment() {
        Payment payment = createPayment();

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getConfirmedAt()).isNotNull();
    }

    @Test
    void shouldBeIdempotentWhenConfirmingAlreadyConfirmedPayment() {
        Payment payment = createPayment();
        payment.confirm();
        var initialConfirmedAt = payment.getConfirmedAt();

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getConfirmedAt()).isEqualTo(initialConfirmedAt);
    }

    @Test
    void shouldThrowExceptionWhenConfirmingFailedPayment() {
        Payment payment = createPayment();
        payment.fail();

        assertThatThrownBy(payment::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot confirm a failed payment");
    }

    @Test
    void shouldFailPendingPayment() {
        Payment payment = createPayment();

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldBeIdempotentWhenFailingAlreadyFailedPayment() {
        Payment payment = createPayment();
        payment.fail();

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldThrowExceptionWhenFailingConfirmedPayment() {
        Payment payment = createPayment();
        payment.confirm();

        assertThatThrownBy(payment::fail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot fail a confirmed payment");
    }

    private Payment createPayment() {
        return Payment.create(
                new PaymentId(UUID.randomUUID()),
                UUID.randomUUID(),
                "ext-123",
                new IdempotencyKey("idem-123"),
                1000,
                PaymentMethod.CREDIT_CARD
        );
    }
}
