package br.com.assine.access.domain.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

public interface GrantAccessUseCase {
    void execute(Command command);

    record Command(
        UUID userId,
        String resource,
        UUID subscriptionId,
        LocalDateTime expiresAt
    ) {}
}
