package br.com.assine.billing.domain.model;

import java.util.Objects;

public record IdempotencyKey(String value) {
    public IdempotencyKey {
        Objects.requireNonNull(value, "IdempotencyKey value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey value cannot be blank");
        }
    }
}
