package br.com.assine.fiscal.adapter.out.messaging;

import br.com.assine.fiscal.domain.event.InvoiceFailed;
import br.com.assine.fiscal.domain.event.InvoiceIssued;
import br.com.assine.fiscal.domain.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class InvoiceEventPublisherAdapter implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "assine.invoice";
    public static final String ROUTING_KEY_ISSUED = "assine.invoice.issued";
    public static final String ROUTING_KEY_FAILED = "assine.invoice.failed";

    public InvoiceEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishIssued(InvoiceIssued event) {
        log.info("Publishing InvoiceIssued event for paymentId: {}", event.paymentId());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_ISSUED, event);
    }

    @Override
    public void publishFailed(InvoiceFailed event) {
        log.info("Publishing InvoiceFailed event for paymentId: {}", event.paymentId());
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_FAILED, event);
    }
}
