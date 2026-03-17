package br.com.assine.auth.adapter.out.persistence;

import br.com.assine.auth.domain.model.AuthProvider;
import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.AuthUserId;
import br.com.assine.auth.domain.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "auth_users",
        indexes = {
                @Index(name = "idx_auth_users_email", columnList = "email", unique = true),
                @Index(name = "idx_auth_users_role", columnList = "role")
        }
)
public class AuthUserEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    protected AuthUserEntity() {
    }

    private AuthUserEntity(String email, AuthProvider provider, UserRole role, LocalDateTime lastLogin) {
        this.email = email;
        this.provider = provider;
        this.role = role;
        this.lastLogin = lastLogin;
    }

    public static AuthUserEntity fromDomain(AuthUser authUser) {
        AuthUserEntity entity = new AuthUserEntity(
                authUser.email(),
                authUser.provider(),
                authUser.role(),
                authUser.lastLogin()
        );

        if (authUser.id() != null) {
            entity.setId(authUser.id().value());
        }

        return entity;
    }

    public AuthUser toDomain() {
        return new AuthUser(
                getId() == null ? null : new AuthUserId(getId()),
                email,
                provider,
                role,
                lastLogin
        );
    }
}
