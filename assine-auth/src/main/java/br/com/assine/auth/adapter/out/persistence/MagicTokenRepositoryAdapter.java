package br.com.assine.auth.adapter.out.persistence;

import br.com.assine.auth.adapter.out.persistence.repository.MagicTokenJpaRepository;
import br.com.assine.auth.domain.model.MagicToken;
import br.com.assine.auth.domain.port.out.MagicTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MagicTokenRepositoryAdapter implements MagicTokenRepository {

    private final MagicTokenJpaRepository magicTokenJpaRepository;

    public MagicTokenRepositoryAdapter(MagicTokenJpaRepository magicTokenJpaRepository) {
        this.magicTokenJpaRepository = magicTokenJpaRepository;
    }

    @Override
    public Optional<MagicToken> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        return magicTokenJpaRepository.findById(id).map(MagicTokenEntity::toDomain);
    }

    @Override
    public Optional<MagicToken> findByToken(String token) {
        Objects.requireNonNull(token, "token must not be null");

        return magicTokenJpaRepository.findByToken(token.trim()).map(MagicTokenEntity::toDomain);
    }

    @Override
    public MagicToken save(MagicToken magicToken) {
        Objects.requireNonNull(magicToken, "magicToken must not be null");

        return magicTokenJpaRepository.save(MagicTokenEntity.fromDomain(magicToken)).toDomain();
    }
}
