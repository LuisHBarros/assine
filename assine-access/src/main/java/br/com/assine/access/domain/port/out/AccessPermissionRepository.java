package br.com.assine.access.domain.port.out;

import br.com.assine.access.domain.model.AccessPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessPermissionRepository {
    AccessPermission save(AccessPermission permission);
    List<AccessPermission> findByUserIdAndResource(UUID userId, String resource);
    List<AccessPermission> findBySubscriptionId(UUID subscriptionId);
}
