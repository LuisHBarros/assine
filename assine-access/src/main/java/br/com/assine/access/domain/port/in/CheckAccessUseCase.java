package br.com.assine.access.domain.port.in;

import java.util.UUID;

public interface CheckAccessUseCase {
    boolean hasAccess(UUID userId, String resource);
}
