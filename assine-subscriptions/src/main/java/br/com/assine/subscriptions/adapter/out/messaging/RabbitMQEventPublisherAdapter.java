package br.com.assine.subscriptions.adapter.out.messaging;

import br.com.assine.subscriptions.domain.event.DomainEvent;
import br.com.assine.subscriptions.domain.event.SubscriptionActivated;
import br.com.assine.subscriptions.domain.event.SubscriptionCanceled;
import br.com.assine.subscriptions.domain.port.out.DomainEventPublisher;
import br.com.assine.subscriptions.infrastructure.messaging.RabbitMqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQEventPublisherAdapter implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisherAdapter.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitMQEventPublisherAdapter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String routingKey = determineRoutingKey(event);
            String payload = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, routingKey, payload, message -> {
                message.getMessageProperties().setContentType("application/json");
                return message;
            });

            log.info("Published event {} with routing key {}", event.getClass().getSimpleName(), routingKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event, e);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event, e);
        }
    }

    private String determineRoutingKey(DomainEvent event) {
        if (event instanceof SubscriptionActivated) {
            return "assine.subscription.activated";
        } else if (event instanceof SubscriptionCanceled) {
            return "assine.subscription.canceled";
        }
        return "assine.subscription.unknown";
    }
}
