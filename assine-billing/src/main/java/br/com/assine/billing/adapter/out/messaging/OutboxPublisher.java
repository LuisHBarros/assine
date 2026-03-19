package br.com.assine.billing.adapter.out.messaging;

import br.com.assine.billing.adapter.out.persistence.OutboxEventEntity;
import br.com.assine.billing.adapter.out.persistence.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String EXCHANGE_NAME = "assine.events";

    private final OutboxJpaRepository outboxJpaRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxJpaRepository outboxJpaRepository, RabbitTemplate rabbitTemplate) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> pendingEvents = outboxJpaRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events", pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                String routingKey = determineRoutingKey(event.getEventType());
                
                // Set message properties and correlationId if needed, but for simplicity we send the JSON string
                rabbitTemplate.convertAndSend(EXCHANGE_NAME, routingKey, event.getPayload(), message -> {
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                });

                event.setPublished(true);
                outboxJpaRepository.save(event);

                log.debug("Published event {} with routing key {}", event.getId(), routingKey);
            } catch (Exception e) {
                log.error("Failed to publish event {}", event.getId(), e);
            }
        }
    }

    private String determineRoutingKey(String eventType) {
        return switch (eventType) {
            case "PaymentConfirmed" -> "assine.payment.confirmed";
            case "PaymentFailed" -> "assine.payment.failed";
            case "ChargebackCreated" -> "assine.payment.chargeback-created";
            default -> "assine.events.unknown";
        };
    }
}
