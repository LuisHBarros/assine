package br.com.assine.access.application.usecase;

import br.com.assine.access.domain.model.AccessPermission;
import br.com.assine.access.domain.port.in.CheckAccessUseCase;
import br.com.assine.access.domain.port.out.AccessPermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CheckAccessService implements CheckAccessUseCase {
    private final AccessPermissionRepository repository;

    public CheckAccessService(AccessPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean hasAccess(UUID userId, String resource) {
        List<AccessPermission> permissions = repository.findByUserIdAndResource(userId, resource);
        return permissions.stream().anyMatch(AccessPermission::isActive);
    }
}
