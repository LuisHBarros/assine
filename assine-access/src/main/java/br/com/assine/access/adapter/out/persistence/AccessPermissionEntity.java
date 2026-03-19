package br.com.assine.access.adapter.out.persistence;

import br.com.assine.access.domain.model.AccessPermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "access_permissions")
public class AccessPermissionEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String resource;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AccessPermissionEntity() {}

    public static AccessPermissionEntity fromDomain(AccessPermission permission) {
        AccessPermissionEntity entity = new AccessPermissionEntity();
        entity.setId(permission.getId());
        entity.userId = permission.getUserId();
        entity.resource = permission.getResource();
        entity.subscriptionId = permission.getSubscriptionId();
        entity.expiresAt = permission.getExpiresAt();
        entity.revokedAt = permission.getRevokedAt();
        entity.createdAt = permission.getCreatedAt();
        return entity;
    }

    public AccessPermission toDomain() {
        return new AccessPermission(
            getId(),
            userId,
            resource,
            subscriptionId,
            expiresAt,
            revokedAt,
            createdAt
        );
    }
}
