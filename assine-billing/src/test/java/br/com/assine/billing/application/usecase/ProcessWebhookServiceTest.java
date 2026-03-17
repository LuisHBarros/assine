package br.com.assine.billing.application.usecase;

import br.com.assine.billing.domain.event.PaymentConfirmed;
import br.com.assine.billing.domain.event.PaymentFailed;
import br.com.assine.billing.domain.model.IdempotencyKey;
import br.com.assine.billing.domain.model.Payment;
import br.com.assine.billing.domain.model.PaymentId;
import br.com.assine.billing.domain.model.PaymentMethod;
import br.com.assine.billing.domain.model.PaymentStatus;
import br.com.assine.billing.domain.port.out.OutboxRepository;
import br.com.assine.billing.domain.port.out.PaymentGateway;
import br.com.assine.billing.domain.port.out.PaymentRepository;
import br.com.assine.billing.domain.port.out.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessWebhookServiceTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private ProcessWebhookService processWebhookService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    @BeforeEach
    void setUp() {
        processWebhookService = new ProcessWebhookService(
                paymentGateway,
                webhookEventRepository,
                paymentRepository,
                outboxRepository
        );
    }

    @Test
    void shouldIgnoreUnsupportedEvent() {
        String payload = "{}";
        String signature = "sig";
        when(paymentGateway.parseEvent(payload, signature))
                .thenReturn(new PaymentGateway.ParsedWebhookEvent("evt-1", PaymentGateway.WebhookEventType.UNSUPPORTED, null, null));

        processWebhookService.process(payload, signature);

        verify(webhookEventRepository, never()).exists(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreAlreadyProcessedEvent() {
        String payload = "{}";
        String signature = "sig";
        when(paymentGateway.parseEvent(payload, signature))
                .thenReturn(new PaymentGateway.ParsedWebhookEvent("evt-1", PaymentGateway.WebhookEventType.PAYMENT_CONFIRMED, null, null));
        when(webhookEventRepository.exists("evt-1")).thenReturn(true);

        processWebhookService.process(payload, signature);

        verify(webhookEventRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldProcessNewPaymentConfirmation() {
        String payload = "{}";
        String signature = "sig";
        Payment payment = new Payment(null, UUID.randomUUID(), "ext-1", new IdempotencyKey("idem-1"), 1000, PaymentMethod.CREDIT_CARD, PaymentStatus.PENDING, null, null);
        
        when(paymentGateway.parseEvent(payload, signature))
                .thenReturn(new PaymentGateway.ParsedWebhookEvent("evt-1", PaymentGateway.WebhookEventType.PAYMENT_CONFIRMED, payment, null));
        when(webhookEventRepository.exists("evt-1")).thenReturn(false);
        when(paymentRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) {
                return new Payment(new PaymentId(UUID.randomUUID()), p.getSubscriptionId(), p.getExternalId(), p.getIdempotencyKey(), p.getAmountCents(), p.getPaymentMethod(), p.getStatus(), p.getConfirmedAt(), p.getCreatedAt());
            }
            return p;
        });

        processWebhookService.process(payload, signature);

        verify(webhookEventRepository).save("evt-1");
        verify(paymentRepository, times(2)).save(paymentCaptor.capture()); // First to assign ID, second after state change

        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus().name()).isEqualTo("CONFIRMED");

        verify(outboxRepository).save(any(PaymentConfirmed.class), eq(savedPayment.getId().value()));
    }

    @Test
    void shouldProcessExistingPaymentFailure() {
        String payload = "{}";
        String signature = "sig";
        Payment existingPayment = Payment.create(new PaymentId(UUID.randomUUID()), UUID.randomUUID(), "ext-1", new IdempotencyKey("idem-1"), 1000, PaymentMethod.CREDIT_CARD);
        
        when(paymentGateway.parseEvent(payload, signature))
                .thenReturn(new PaymentGateway.ParsedWebhookEvent("evt-1", PaymentGateway.WebhookEventType.PAYMENT_FAILED, existingPayment, "Card Declined"));
        when(webhookEventRepository.exists("evt-1")).thenReturn(false);
        when(paymentRepository.findByExternalId("ext-1")).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        processWebhookService.process(payload, signature);

        verify(webhookEventRepository).save("evt-1");
        verify(paymentRepository, times(1)).save(paymentCaptor.capture()); 

        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus().name()).isEqualTo("FAILED");

        verify(outboxRepository).save(any(PaymentFailed.class), eq(savedPayment.getId().value()));
    }
}
