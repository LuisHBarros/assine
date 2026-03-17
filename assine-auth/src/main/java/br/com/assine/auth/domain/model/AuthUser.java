package br.com.assine.auth.domain.model;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

public record AuthUser(
        AuthUserId id,
        String email,
        AuthProvider provider,
        UserRole role,
        LocalDateTime lastLogin
) {

    public AuthUser {
        email = normalizeEmail(email);
        provider = Objects.requireNonNull(provider, "provider must not be null");
        role = role == null ? UserRole.USER : role;
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
