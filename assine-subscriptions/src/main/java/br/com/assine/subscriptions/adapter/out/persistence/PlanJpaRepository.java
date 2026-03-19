package br.com.assine.subscriptions.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlanJpaRepository extends JpaRepository<PlanEntity, UUID> {
}
