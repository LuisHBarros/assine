package br.com.assine.subscriptions.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, UUID> {
    List<SubscriptionEntity> findByUserId(UUID userId);
}
