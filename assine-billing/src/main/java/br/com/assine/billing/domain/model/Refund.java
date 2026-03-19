package br.com.assine.billing.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Refund {
    private UUID id;
    private PaymentId paymentId;
    private UUID subscriptionId;
    private String externalId;
    private LocalDateTime requestedAt;
    private LocalDate activatedAt;
    private Integer daysSinceActivation;
    private Double refundPercentage;
    private Integer originalAmountCents;
    private Integer refundAmountCents;
    private RefundStatus status;
    private String reason;
    private LocalDateTime completedAt;

    public Refund(UUID id, PaymentId paymentId, UUID subscriptionId, String externalId,
                  LocalDateTime requestedAt, LocalDate activatedAt, Integer daysSinceActivation,
                  Double refundPercentage, Integer originalAmountCents, Integer refundAmountCents,
                  RefundStatus status, String reason, LocalDateTime completedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
        this.externalId = externalId;
        this.requestedAt = requestedAt;
        this.activatedAt = activatedAt;
        this.daysSinceActivation = daysSinceActivation;
        this.refundPercentage = refundPercentage;
        this.originalAmountCents = originalAmountCents;
        this.refundAmountCents = refundAmountCents;
        this.status = status;
        this.reason = reason;
        this.completedAt = completedAt;
    }

    public static Refund create(UUID id, PaymentId paymentId, UUID subscriptionId, 
                                LocalDate activatedAt, Integer daysSinceActivation, 
                                Double refundPercentage, Integer originalAmountCents, 
                                Integer refundAmountCents, String reason) {
        return new Refund(id, paymentId, subscriptionId, null, LocalDateTime.now(), 
                          activatedAt, daysSinceActivation, refundPercentage, 
                          originalAmountCents, refundAmountCents, RefundStatus.PENDING, 
                          reason, null);
    }

    public void complete(String externalId) {
        this.status = RefundStatus.COMPLETED;
        this.externalId = externalId;
        this.completedAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public PaymentId getPaymentId() { return paymentId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getExternalId() { return externalId; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDate getActivatedAt() { return activatedAt; }
    public Integer getDaysSinceActivation() { return daysSinceActivation; }
    public Double getRefundPercentage() { return refundPercentage; }
    public Integer getOriginalAmountCents() { return originalAmountCents; }
    public Integer getRefundAmountCents() { return refundAmountCents; }
    public RefundStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
