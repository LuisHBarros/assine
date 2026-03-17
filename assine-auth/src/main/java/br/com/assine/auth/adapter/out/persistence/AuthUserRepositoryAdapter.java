package br.com.assine.auth.adapter.out.persistence;

import br.com.assine.auth.adapter.out.persistence.repository.AuthUserJpaRepository;
import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.AuthUserId;
import br.com.assine.auth.domain.port.out.AuthUserRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class AuthUserRepositoryAdapter implements AuthUserRepository {

    private final AuthUserJpaRepository authUserJpaRepository;

    public AuthUserRepositoryAdapter(AuthUserJpaRepository authUserJpaRepository) {
        this.authUserJpaRepository = authUserJpaRepository;
    }

    @Override
    public Optional<AuthUser> findById(AuthUserId id) {
        Objects.requireNonNull(id, "id must not be null");

        return authUserJpaRepository.findById(id.value()).map(AuthUserEntity::toDomain);
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return authUserJpaRepository.findByEmail(email).map(AuthUserEntity::toDomain);
    }

    @Override
    public AuthUser save(AuthUser authUser) {
        Objects.requireNonNull(authUser, "authUser must not be null");

        return authUserJpaRepository.save(AuthUserEntity.fromDomain(authUser)).toDomain();
    }
}
