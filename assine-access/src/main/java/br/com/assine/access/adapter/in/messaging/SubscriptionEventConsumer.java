package br.com.assine.access.adapter.in.messaging;

import br.com.assine.access.domain.port.in.GrantAccessUseCase;
import br.com.assine.access.domain.port.in.RevokeAccessUseCase;
import br.com.assine.access.infrastructure.messaging.RabbitMqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class SubscriptionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumer.class);
    private final GrantAccessUseCase grantAccessUseCase;
    private final RevokeAccessUseCase revokeAccessUseCase;
    private final ObjectMapper objectMapper;

    public SubscriptionEventConsumer(GrantAccessUseCase grantAccessUseCase, RevokeAccessUseCase revokeAccessUseCase, ObjectMapper objectMapper) {
        this.grantAccessUseCase = grantAccessUseCase;
        this.revokeAccessUseCase = revokeAccessUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMqConfig.SUBSCRIPTION_ACTIVATED_QUEUE)
    public void handleSubscriptionActivated(String messagePayload) {
        log.info("Received SubscriptionActivated event: {}", messagePayload);
        try {
            JsonNode event = objectMapper.readTree(messagePayload);
            UUID subscriptionId = UUID.fromString(event.path("subscriptionId").asText());
            UUID userId = UUID.fromString(event.path("userId").asText());
            
            LocalDateTime expiresAt = null;
            if (event.hasNonNull("currentPeriodEnd")) {
                expiresAt = LocalDateTime.parse(event.path("currentPeriodEnd").asText(), DateTimeFormatter.ISO_DATE_TIME);
            }

            GrantAccessUseCase.Command command = new GrantAccessUseCase.Command(
                userId,
                "newsletter", // Default resource for basic subscription
                subscriptionId,
                expiresAt
            );
            
            grantAccessUseCase.execute(command);
            log.info("Access granted for subscription {}", subscriptionId);
        } catch (Exception e) {
            log.error("Error processing SubscriptionActivated event", e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.SUBSCRIPTION_CANCELED_QUEUE)
    public void handleSubscriptionCanceled(String messagePayload) {
        log.info("Received SubscriptionCanceled event: {}", messagePayload);
        try {
            JsonNode event = objectMapper.readTree(messagePayload);
            UUID subscriptionId = UUID.fromString(event.path("subscriptionId").asText());

            revokeAccessUseCase.execute(subscriptionId);
            log.info("Access revoked for subscription {}", subscriptionId);
        } catch (Exception e) {
            log.error("Error processing SubscriptionCanceled event", e);
        }
    }
}
