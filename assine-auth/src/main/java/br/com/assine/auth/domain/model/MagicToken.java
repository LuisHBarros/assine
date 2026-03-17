package br.com.assine.auth.domain.model;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record MagicToken(
        UUID id,
        String email,
        String token,
        LocalDateTime expiresAt,
        boolean used
) {

    public MagicToken {
        email = normalizeEmail(email);
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        Objects.requireNonNull(token, "token must not be null");
        token = token.trim();

        if (token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
    }

    private static String normalizeEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        return normalized;
    }
}
