package br.com.assine.access.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccessPermission {
    private final UUID id;
    private final UUID userId;
    private final String resource;
    private final UUID subscriptionId;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private final LocalDateTime createdAt;

    public AccessPermission(UUID id, UUID userId, String resource, UUID subscriptionId,
                            LocalDateTime expiresAt, LocalDateTime revokedAt, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.resource = resource;
        this.subscriptionId = subscriptionId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public static AccessPermission grant(UUID userId, String resource, UUID subscriptionId, LocalDateTime expiresAt) {
        return new AccessPermission(
            UUID.randomUUID(),
            userId,
            resource,
            subscriptionId,
            expiresAt,
            null,
            LocalDateTime.now()
        );
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        if (revokedAt != null) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getResource() { return resource; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
