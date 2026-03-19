package br.com.assine.billing.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
    private PaymentId id;
    private UUID subscriptionId;
    private String externalId;
    private IdempotencyKey idempotencyKey;
    private Integer amountCents;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;

    public Payment(PaymentId id, UUID subscriptionId, String externalId, IdempotencyKey idempotencyKey,
                   Integer amountCents, PaymentMethod paymentMethod, PaymentStatus status,
                   LocalDateTime confirmedAt, LocalDateTime createdAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.externalId = externalId;
        this.idempotencyKey = idempotencyKey;
        this.amountCents = amountCents;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.confirmedAt = confirmedAt;
        this.createdAt = createdAt;
    }

    public static Payment create(PaymentId id, UUID subscriptionId, String externalId, 
                                 IdempotencyKey idempotencyKey, Integer amountCents, 
                                 PaymentMethod paymentMethod) {
        return new Payment(id, subscriptionId, externalId, idempotencyKey, amountCents, 
                           paymentMethod, PaymentStatus.PENDING, null, LocalDateTime.now());
    }

    public void confirm() {
        if (this.status == PaymentStatus.CONFIRMED) {
            return; // Idempotent operation
        }
        if (this.status == PaymentStatus.FAILED) {
            throw new IllegalStateException("Cannot confirm a failed payment");
        }
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.status == PaymentStatus.FAILED) {
            return; // Idempotent operation
        }
        if (this.status == PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot fail a confirmed payment");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        if (this.status == PaymentStatus.REFUNDED) {
            return; // Idempotent
        }
        if (this.status != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed payments can be refunded");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public PaymentId getId() { return id; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getExternalId() { return externalId; }
    public IdempotencyKey getIdempotencyKey() { return idempotencyKey; }
    public Integer getAmountCents() { return amountCents; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
