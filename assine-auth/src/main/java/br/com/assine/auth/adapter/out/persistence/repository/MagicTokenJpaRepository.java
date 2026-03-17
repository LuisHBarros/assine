package br.com.assine.auth.adapter.out.persistence.repository;

import br.com.assine.auth.adapter.out.persistence.MagicTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MagicTokenJpaRepository extends JpaRepository<MagicTokenEntity, UUID> {

    Optional<MagicTokenEntity> findByToken(String token);
}
