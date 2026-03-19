package br.com.assine.billing.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Chargeback {
    private UUID id;
    private PaymentId paymentId;
    private UUID subscriptionId;
    private String externalId;
    private Integer amountCents;
    private ChargebackStatus status;
    private LocalDateTime openedAt;
    private LocalDateTime resolvedAt;

    public Chargeback(UUID id, PaymentId paymentId, UUID subscriptionId, String externalId,
                      Integer amountCents, ChargebackStatus status, LocalDateTime openedAt,
                      LocalDateTime resolvedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
        this.externalId = externalId;
        this.amountCents = amountCents;
        this.status = status;
        this.openedAt = openedAt;
        this.resolvedAt = resolvedAt;
    }

    public static Chargeback create(UUID id, PaymentId paymentId, UUID subscriptionId,
                                    String externalId, Integer amountCents) {
        return new Chargeback(id, paymentId, subscriptionId, externalId, amountCents,
                ChargebackStatus.OPEN, LocalDateTime.now(), null);
    }

    public void resolve(ChargebackStatus newStatus) {
        this.status = newStatus;
        this.resolvedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public PaymentId getPaymentId() { return paymentId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getExternalId() { return externalId; }
    public Integer getAmountCents() { return amountCents; }
    public ChargebackStatus getStatus() { return status; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
