package br.com.assine.access.application.usecase;

import br.com.assine.access.domain.model.AccessPermission;
import br.com.assine.access.domain.port.in.GrantAccessUseCase;
import br.com.assine.access.domain.port.out.AccessPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantAccessService implements GrantAccessUseCase {
    private final AccessPermissionRepository repository;

    public GrantAccessService(AccessPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        AccessPermission permission = AccessPermission.grant(
            command.userId(),
            command.resource(),
            command.subscriptionId(),
            command.expiresAt()
        );
        repository.save(permission);
    }
}
