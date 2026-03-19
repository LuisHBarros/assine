package br.com.assine.subscriptions.domain.port.out;

import br.com.assine.subscriptions.domain.model.Plan;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {
    Optional<Plan> findById(UUID id);
}
