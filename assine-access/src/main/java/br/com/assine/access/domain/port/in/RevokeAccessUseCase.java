package br.com.assine.access.domain.port.in;

import java.util.UUID;

public interface RevokeAccessUseCase {
    void execute(UUID subscriptionId);
}
