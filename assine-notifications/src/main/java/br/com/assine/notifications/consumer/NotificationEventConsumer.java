package br.com.assine.notifications.consumer;

import br.com.assine.notifications.config.RabbitMQConfig;
import br.com.assine.notifications.domain.event.MagicLinkRequestedEvent;
import br.com.assine.notifications.domain.event.PaymentConfirmedEvent;
import br.com.assine.notifications.domain.event.SubscriptionActivatedEvent;
import br.com.assine.notifications.domain.port.out.EmailPort;
import br.com.assine.notifications.template.EmailTemplateResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final EmailPort emailPort;
    private final EmailTemplateResolver templateResolver;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(EmailPort emailPort, EmailTemplateResolver templateResolver, ObjectMapper objectMapper) {
        this.emailPort = emailPort;
        this.templateResolver = templateResolver;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onMessage(Message message, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        String payload = new String(message.getBody());
        log.info("Received event from routing key {}: {}", routingKey, payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String correlationId = jsonNode.has("correlationId") ? jsonNode.get("correlationId").asText() : null;
            setCorrelationId(correlationId);

            if (routingKey.equals("assine.payment.confirmed")) {
                handlePaymentConfirmed(jsonNode, correlationId);
            } else if (routingKey.startsWith("assine.subscription.activated")) {
                handleSubscriptionActivated(jsonNode, correlationId);
            } else if (routingKey.equals("magic-link-requested")) {
                handleMagicLinkRequested(jsonNode, correlationId);
            } else {
                log.warn("Unknown routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process event from routing key {}: {}", routingKey, payload, e);
        } finally {
            clearCorrelationId();
        }
    }

    private void handleMagicLinkRequested(JsonNode jsonNode, String correlationId) {
        String email = jsonNode.get("email").asText();
        String token = jsonNode.get("token").asText();
        MagicLinkRequestedEvent event = new MagicLinkRequestedEvent(email, token, correlationId);
        
        log.info("Processing magic link requested event for {}", event.email());
        String body = templateResolver.resolveMagicLinkBody(event);
        emailPort.send(event.email(), "Seu link de acesso ao Assine", body);
    }

    private void handlePaymentConfirmed(JsonNode jsonNode, String correlationId) {
        // According to assine-billing, it has paymentId and subscriptionId
        // but it might also have userEmail etc if we want to send email
        // For now let's assume it has what we need or we use defaults
        
        UUID paymentId = jsonNode.has("paymentId") ? UUID.fromString(jsonNode.get("paymentId").asText()) : null;
        UUID subscriptionId = jsonNode.has("subscriptionId") ? UUID.fromString(jsonNode.get("subscriptionId").asText()) : null;
        
        String userEmail = jsonNode.has("userEmail") ? jsonNode.get("userEmail").asText() : "user@example.com";
        String userName = jsonNode.has("userName") ? jsonNode.get("userName").asText() : "Cliente";
        String amount = jsonNode.has("amount") ? jsonNode.get("amount").asText() : "0.00";

        PaymentConfirmedEvent event = new PaymentConfirmedEvent(paymentId, subscriptionId, userEmail, userName, amount, correlationId);
        
        log.info("Processing payment confirmed event for subscription {}", event.subscriptionId());
        String body = templateResolver.resolvePaymentConfirmedBody(event);
        emailPort.send(event.userEmail(), "Pagamento confirmado!", body);
    }

    private void handleSubscriptionActivated(JsonNode jsonNode, String correlationId) {
        UUID subscriptionId = jsonNode.has("subscriptionId") ? UUID.fromString(jsonNode.get("subscriptionId").asText()) : null;
        UUID userId = jsonNode.has("userId") ? UUID.fromString(jsonNode.get("userId").asText()) : null;
        
        String userEmail = jsonNode.has("userEmail") ? jsonNode.get("userEmail").asText() : "user@example.com";
        String userName = jsonNode.has("userName") ? jsonNode.get("userName").asText() : "Cliente";
        String planName = jsonNode.has("planName") ? jsonNode.get("planName").asText() : "Plano Assine";

        SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(subscriptionId, userId, userEmail, userName, planName, correlationId);
        
        log.info("Processing subscription activated event for user {}", event.userId());
        String body = templateResolver.resolveSubscriptionActivatedBody(event);
        emailPort.send(event.userEmail(), "Bem-vindo ao Assine - Assinatura Ativa", body);
    }

    private void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
    }

    private void clearCorrelationId() {
        MDC.remove("correlationId");
    }
}
