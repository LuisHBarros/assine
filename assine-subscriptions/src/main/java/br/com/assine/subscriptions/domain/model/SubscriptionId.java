package br.com.assine.subscriptions.domain.model;

import java.util.UUID;

public record SubscriptionId(UUID value) {
    public static SubscriptionId generate() {
        return new SubscriptionId(UUID.randomUUID());
    }

    public static SubscriptionId fromUUID(UUID value) {
        return new SubscriptionId(value);
    }
}
