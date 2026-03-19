package br.com.assine.fiscal.adapter.in.messaging;

import br.com.assine.fiscal.domain.port.in.IssueInvoiceUseCase;
import br.com.assine.fiscal.infrastructure.messaging.RabbitMQConfig;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedConsumer.class);

    private final IssueInvoiceUseCase issueInvoiceUseCase;

    public PaymentConfirmedConsumer(IssueInvoiceUseCase issueInvoiceUseCase) {
        this.issueInvoiceUseCase = issueInvoiceUseCase;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CONFIRMED_QUEUE)
    public void consume(PaymentConfirmedEvent event) {
        log.info("Consumed PaymentConfirmed event for paymentId: {}", event.paymentId());

        issueInvoiceUseCase.handleConfirmedPayment(
            event.paymentId(),
            event.subscriptionId(),
            event.userId(),
            event.amountCents()
        );
    }

    public record PaymentConfirmedEvent(
        UUID paymentId,
        UUID subscriptionId,
        UUID userId,
        Integer amountCents
    ) {}
}
