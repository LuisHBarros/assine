package br.com.assine.notifications.template;

import br.com.assine.notifications.domain.event.MagicLinkRequestedEvent;
import br.com.assine.notifications.domain.event.PaymentConfirmedEvent;
import br.com.assine.notifications.domain.event.SubscriptionActivatedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateResolverTest {

    private final EmailTemplateResolver resolver = new EmailTemplateResolver();

    @Test
    void shouldResolveMagicLinkBody() {
        MagicLinkRequestedEvent event = new MagicLinkRequestedEvent("test@example.com", "token123", "corr-1");
        String body = resolver.resolveMagicLinkBody(event);
        assertTrue(body.contains("token123"));
    }

    @Test
    void shouldResolvePaymentConfirmedBody() {
        UUID subId = UUID.randomUUID();
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(UUID.randomUUID(), subId, "test@example.com", "John Doe", "99.90", "corr-2");
        String body = resolver.resolvePaymentConfirmedBody(event);
        assertTrue(body.contains("John Doe"));
        assertTrue(body.contains("99.90"));
        assertTrue(body.contains(subId.toString()));
    }

    @Test
    void shouldResolveSubscriptionActivatedBody() {
        SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(UUID.randomUUID(), UUID.randomUUID(), "test@example.com", "John Doe", "Premium", "corr-3");
        String body = resolver.resolveSubscriptionActivatedBody(event);
        assertTrue(body.contains("John Doe"));
        assertTrue(body.contains("Premium"));
    }
}
