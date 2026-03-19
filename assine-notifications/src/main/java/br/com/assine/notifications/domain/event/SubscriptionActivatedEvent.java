package br.com.assine.notifications.domain.event;

import java.util.UUID;

public record SubscriptionActivatedEvent(
    UUID subscriptionId,
    UUID userId,
    String userEmail,
    String userName,
    String planName,
    String correlationId
) {}
