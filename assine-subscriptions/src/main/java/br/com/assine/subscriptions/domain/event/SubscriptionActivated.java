package br.com.assine.subscriptions.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionActivated(
    UUID subscriptionId,
    UUID userId,
    UUID planId,
    LocalDateTime createdAt
) implements DomainEvent {
    @Override
    public String getEventType() { return "SubscriptionActivated"; }
    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }
}
