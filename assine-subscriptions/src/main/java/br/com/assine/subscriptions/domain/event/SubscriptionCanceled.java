package br.com.assine.subscriptions.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionCanceled(
    UUID subscriptionId,
    UUID userId,
    LocalDateTime createdAt
) implements DomainEvent {
    @Override
    public String getEventType() { return "SubscriptionCanceled"; }
    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }
}
