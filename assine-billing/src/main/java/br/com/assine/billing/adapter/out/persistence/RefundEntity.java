package br.com.assine.billing.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class RefundEntity extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "activated_at", nullable = false)
    private LocalDate activatedAt;

    @Column(name = "days_since_activation", nullable = false)
    private Integer daysSinceActivation;

    @Column(name = "refund_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    @Column(name = "original_amount_cents", nullable = false)
    private Integer originalAmountCents;

    @Column(name = "refund_amount_cents", nullable = false)
    private Integer refundAmountCents;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public RefundEntity() {}

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDate getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDate activatedAt) { this.activatedAt = activatedAt; }

    public Integer getDaysSinceActivation() { return daysSinceActivation; }
    public void setDaysSinceActivation(Integer daysSinceActivation) { this.daysSinceActivation = daysSinceActivation; }

    public BigDecimal getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(BigDecimal refundPercentage) { this.refundPercentage = refundPercentage; }

    public Integer getOriginalAmountCents() { return originalAmountCents; }
    public void setOriginalAmountCents(Integer originalAmountCents) { this.originalAmountCents = originalAmountCents; }

    public Integer getRefundAmountCents() { return refundAmountCents; }
    public void setRefundAmountCents(Integer refundAmountCents) { this.refundAmountCents = refundAmountCents; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
