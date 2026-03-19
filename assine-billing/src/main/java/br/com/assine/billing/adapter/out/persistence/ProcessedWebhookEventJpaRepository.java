package br.com.assine.billing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookEventJpaRepository extends JpaRepository<ProcessedWebhookEventEntity, String> {
}
