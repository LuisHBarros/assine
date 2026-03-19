package br.com.assine.subscriptions.domain.model;

import java.util.UUID;

public record UserId(UUID value) {
    public static UserId fromUUID(UUID value) {
        return new UserId(value);
    }
}
