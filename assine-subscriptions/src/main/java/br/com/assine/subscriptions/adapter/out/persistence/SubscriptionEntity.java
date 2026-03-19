package br.com.assine.subscriptions.adapter.out.persistence;

import br.com.assine.subscriptions.domain.model.Subscription;
import br.com.assine.subscriptions.domain.model.SubscriptionId;
import br.com.assine.subscriptions.domain.model.SubscriptionStatus;
import br.com.assine.subscriptions.domain.model.UserId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SubscriptionEntity() {}

    public static SubscriptionEntity fromDomain(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(subscription.getId().value());
        entity.userId = subscription.getUserId().value();
        entity.plan = PlanEntity.fromDomain(subscription.getPlan());
        entity.paymentMethod = subscription.getPaymentMethod();
        entity.status = subscription.getStatus();
        entity.currentPeriodEnd = subscription.getCurrentPeriodEnd();
        entity.canceledAt = subscription.getCanceledAt();
        entity.createdAt = subscription.getCreatedAt();
        entity.updatedAt = subscription.getUpdatedAt();
        return entity;
    }

    public Subscription toDomain() {
        return new Subscription(
            SubscriptionId.fromUUID(getId()),
            UserId.fromUUID(userId),
            plan.toDomain(),
            paymentMethod,
            status,
            currentPeriodEnd,
            canceledAt,
            createdAt,
            updatedAt
        );
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public PlanEntity getPlan() { return plan; }
    public String getPaymentMethod() { return paymentMethod; }
    public SubscriptionStatus getStatus() { return status; }
    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
