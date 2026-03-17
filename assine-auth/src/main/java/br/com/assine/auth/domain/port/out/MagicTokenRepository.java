package br.com.assine.auth.domain.port.out;

import br.com.assine.auth.domain.model.MagicToken;

import java.util.Optional;
import java.util.UUID;

public interface MagicTokenRepository {

    Optional<MagicToken> findById(UUID id);

    Optional<MagicToken> findByToken(String token);

    MagicToken save(MagicToken magicToken);
}
