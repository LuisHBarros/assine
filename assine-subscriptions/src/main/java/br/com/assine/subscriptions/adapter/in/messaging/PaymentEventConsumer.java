package br.com.assine.subscriptions.adapter.in.messaging;

import br.com.assine.subscriptions.domain.port.in.ActivateSubscriptionUseCase;
import br.com.assine.subscriptions.infrastructure.messaging.RabbitMqConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final ActivateSubscriptionUseCase activateSubscriptionUseCase;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(ActivateSubscriptionUseCase activateSubscriptionUseCase, ObjectMapper objectMapper) {
        this.activateSubscriptionUseCase = activateSubscriptionUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_CONFIRMED_QUEUE)
    public void handlePaymentConfirmed(String messagePayload) {
        log.info("Received PaymentConfirmed event: {}", messagePayload);
        try {
            JsonNode event = objectMapper.readTree(messagePayload);
            String subscriptionIdStr = event.path("metadata").path("subscription_id").asText();
            
            if (subscriptionIdStr == null || subscriptionIdStr.isEmpty()) {
                log.warn("PaymentConfirmed event without subscription_id in metadata: {}", messagePayload);
                return;
            }

            UUID subscriptionId = UUID.fromString(subscriptionIdStr);
            activateSubscriptionUseCase.execute(subscriptionId);
            log.info("Subscription {} activated via payment event", subscriptionId);
        } catch (Exception e) {
            log.error("Error processing PaymentConfirmed event", e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(String messagePayload) {
        log.info("Received PaymentFailed event: {}", messagePayload);
        // Implement logic to mark as past due if needed
    }
}
