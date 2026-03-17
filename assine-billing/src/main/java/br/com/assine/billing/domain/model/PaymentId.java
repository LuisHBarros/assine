package br.com.assine.billing.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PaymentId(UUID value) {
    public PaymentId {
        Objects.requireNonNull(value, "PaymentId value cannot be null");
    }
}
