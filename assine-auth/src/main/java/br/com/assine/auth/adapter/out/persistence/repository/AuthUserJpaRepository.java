package br.com.assine.auth.adapter.out.persistence.repository;

import br.com.assine.auth.adapter.out.persistence.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, UUID> {

    Optional<AuthUserEntity> findByEmail(String email);
}
