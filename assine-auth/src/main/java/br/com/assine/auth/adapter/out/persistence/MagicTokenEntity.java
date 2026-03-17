package br.com.assine.auth.adapter.out.persistence;

import br.com.assine.auth.domain.model.MagicToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "magic_tokens",
        indexes = {
                @Index(name = "idx_magic_tokens_token", columnList = "token", unique = true),
                @Index(name = "idx_magic_tokens_expires_at", columnList = "expires_at")
        }
)
public class MagicTokenEntity extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    protected MagicTokenEntity() {
    }

    private MagicTokenEntity(String email, String token, LocalDateTime expiresAt, boolean used) {
        this.email = email;
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public static MagicTokenEntity fromDomain(MagicToken magicToken) {
        MagicTokenEntity entity = new MagicTokenEntity(
                magicToken.email(),
                magicToken.token(),
                magicToken.expiresAt(),
                magicToken.used()
        );

        if (magicToken.id() != null) {
            entity.setId(magicToken.id());
        }

        return entity;
    }

    public MagicToken toDomain() {
        return new MagicToken(
                getId(),
                email,
                token,
                expiresAt,
                used
        );
    }
}
