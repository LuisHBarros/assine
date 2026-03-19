package br.com.assine.access.application.usecase;

import br.com.assine.access.domain.model.AccessPermission;
import br.com.assine.access.domain.port.in.RevokeAccessUseCase;
import br.com.assine.access.domain.port.out.AccessPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RevokeAccessService implements RevokeAccessUseCase {
    private final AccessPermissionRepository repository;

    public RevokeAccessService(AccessPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(UUID subscriptionId) {
        List<AccessPermission> permissions = repository.findBySubscriptionId(subscriptionId);
        
        for (AccessPermission permission : permissions) {
            if (permission.isActive()) {
                permission.revoke();
                repository.save(permission);
            }
        }
    }
}
