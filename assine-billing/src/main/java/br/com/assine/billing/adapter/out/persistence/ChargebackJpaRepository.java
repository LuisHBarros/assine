package br.com.assine.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChargebackJpaRepository extends JpaRepository<ChargebackEntity, UUID> {
}
