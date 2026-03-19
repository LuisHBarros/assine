package br.com.assine.billing.application.usecase;

import br.com.assine.billing.domain.event.ChargebackCreated;
import br.com.assine.billing.domain.event.PaymentConfirmed;
import br.com.assine.billing.domain.event.PaymentFailed;
import br.com.assine.billing.domain.model.Chargeback;
import br.com.assine.billing.domain.model.Payment;
import br.com.assine.billing.domain.model.PaymentStatus;
import br.com.assine.billing.domain.port.in.ProcessWebhookUseCase;
import br.com.assine.billing.domain.port.out.ChargebackRepository;
import br.com.assine.billing.domain.port.out.OutboxRepository;
import br.com.assine.billing.domain.port.out.PaymentGateway;
import br.com.assine.billing.domain.port.out.PaymentRepository;
import br.com.assine.billing.domain.port.out.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessWebhookService implements ProcessWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookService.class);

    private final PaymentGateway paymentGateway;
    private final WebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final ChargebackRepository chargebackRepository;
    private final OutboxRepository outboxRepository;

    public ProcessWebhookService(PaymentGateway paymentGateway,
                                 WebhookEventRepository webhookEventRepository,
                                 PaymentRepository paymentRepository,
                                 ChargebackRepository chargebackRepository,
                                 OutboxRepository outboxRepository) {
        this.paymentGateway = paymentGateway;
        this.webhookEventRepository = webhookEventRepository;
        this.paymentRepository = paymentRepository;
        this.chargebackRepository = chargebackRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public void process(String payload, String signature) {
        PaymentGateway.ParsedWebhookEvent parsedEvent = paymentGateway.parseEvent(payload, signature);

        if (parsedEvent.type() == PaymentGateway.WebhookEventType.UNSUPPORTED) {
            log.info("Ignored unsupported webhook event: {}", parsedEvent.eventId());
            return;
        }

        if (webhookEventRepository.exists(parsedEvent.eventId())) {
            log.info("Webhook event already processed: {}", parsedEvent.eventId());
            return;
        }

        webhookEventRepository.save(parsedEvent.eventId());

        Payment incomingData = parsedEvent.partialPaymentData();
        
        if (parsedEvent.type() == PaymentGateway.WebhookEventType.CHARGEBACK_CREATED) {
            // Usually, webhook gives us enough to find the payment
            Payment payment = paymentRepository.findByExternalId(incomingData != null ? incomingData.getExternalId() : "placeholder")
                    .orElseThrow(() -> new IllegalStateException("Payment not found for chargeback"));
            
            Chargeback chargeback = Chargeback.create(UUID.randomUUID(), payment.getId(), payment.getSubscriptionId(),
                    parsedEvent.externalChargebackId(), parsedEvent.chargebackAmountCents());
            
            chargebackRepository.save(chargeback);
            outboxRepository.save(new ChargebackCreated(chargeback.getId(), payment.getId().value(), payment.getSubscriptionId()), chargeback.getId());
            log.info("Chargeback created for payment {}", payment.getId().value());
            return;
        }

        Payment payment = paymentRepository.findByExternalId(incomingData.getExternalId())
                .orElse(incomingData); // If it doesn't exist, use the parsed data as the new entity

        if (payment.getId() == null) {
            // New payment, let's persist it first to generate an ID
            payment = paymentRepository.save(payment);
        }

        boolean statusChanged = false;

        if (parsedEvent.type() == PaymentGateway.WebhookEventType.PAYMENT_CONFIRMED) {
            if (payment.getStatus() != PaymentStatus.CONFIRMED) {
                payment.confirm();
                statusChanged = true;
                outboxRepository.save(new PaymentConfirmed(payment.getId().value(), payment.getSubscriptionId()), payment.getId().value());
            }
        } else if (parsedEvent.type() == PaymentGateway.WebhookEventType.PAYMENT_FAILED) {
            if (payment.getStatus() != PaymentStatus.FAILED) {
                payment.fail();
                statusChanged = true;
                outboxRepository.save(new PaymentFailed(payment.getId().value(), payment.getSubscriptionId(), parsedEvent.failReason()), payment.getId().value());
            }
        }

        if (statusChanged) {
            paymentRepository.save(payment);
            log.info("Payment {} status updated to {}", payment.getId().value(), payment.getStatus());
        }
    }
}
