package br.com.assine.access.adapter.out.persistence;

import br.com.assine.access.domain.model.AccessPermission;
import br.com.assine.access.domain.port.out.AccessPermissionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccessPermissionPersistenceAdapter implements AccessPermissionRepository {

    private final AccessPermissionJpaRepository jpaRepository;

    public AccessPermissionPersistenceAdapter(AccessPermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AccessPermission save(AccessPermission permission) {
        AccessPermissionEntity entity = AccessPermissionEntity.fromDomain(permission);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<AccessPermission> findByUserIdAndResource(UUID userId, String resource) {
        return jpaRepository.findByUserIdAndResource(userId, resource).stream()
                .map(AccessPermissionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccessPermission> findBySubscriptionId(UUID subscriptionId) {
        return jpaRepository.findBySubscriptionId(subscriptionId).stream()
                .map(AccessPermissionEntity::toDomain)
                .collect(Collectors.toList());
    }
}
