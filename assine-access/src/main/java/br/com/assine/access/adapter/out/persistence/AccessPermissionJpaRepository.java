package br.com.assine.access.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AccessPermissionJpaRepository extends JpaRepository<AccessPermissionEntity, UUID> {
    List<AccessPermissionEntity> findByUserIdAndResource(UUID userId, String resource);
    List<AccessPermissionEntity> findBySubscriptionId(UUID subscriptionId);
}
