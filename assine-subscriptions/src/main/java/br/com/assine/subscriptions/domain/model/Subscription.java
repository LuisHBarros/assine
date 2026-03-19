package br.com.assine.subscriptions.domain.model;

import java.time.LocalDateTime;

public class Subscription {
    private final SubscriptionId id;
    private final UserId userId;
    private final Plan plan;
    private final String paymentMethod;
    private SubscriptionStatus status;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime canceledAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Subscription(SubscriptionId id, UserId userId, Plan plan, String paymentMethod,
                        SubscriptionStatus status, LocalDateTime currentPeriodEnd,
                        LocalDateTime canceledAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.plan = plan;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.currentPeriodEnd = currentPeriodEnd;
        this.canceledAt = canceledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subscription create(UserId userId, Plan plan, String paymentMethod) {
        return new Subscription(
            SubscriptionId.generate(),
            userId,
            plan,
            paymentMethod,
            SubscriptionStatus.PENDING,
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        
        if ("MONTHLY".equalsIgnoreCase(plan.interval())) {
            this.currentPeriodEnd = LocalDateTime.now().plusMonths(1);
        } else if ("ANNUAL".equalsIgnoreCase(plan.interval())) {
            this.currentPeriodEnd = LocalDateTime.now().plusYears(1);
        }
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
        this.updatedAt = LocalDateTime.now();
    }

    public SubscriptionId getId() { return id; }
    public UserId getUserId() { return userId; }
    public Plan getPlan() { return plan; }
    public String getPaymentMethod() { return paymentMethod; }
    public SubscriptionStatus getStatus() { return status; }
    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
