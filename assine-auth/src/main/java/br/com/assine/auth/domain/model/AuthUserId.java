package br.com.assine.auth.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AuthUserId(UUID value) {

    public AuthUserId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
