package br.com.assine.subscriptions.adapter.out.persistence;

import br.com.assine.subscriptions.domain.model.Plan;
import br.com.assine.subscriptions.domain.port.out.PlanRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PlanPersistenceAdapter implements PlanRepository {

    private final PlanJpaRepository planJpaRepository;

    public PlanPersistenceAdapter(PlanJpaRepository planJpaRepository) {
        this.planJpaRepository = planJpaRepository;
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        return planJpaRepository.findById(id)
            .map(PlanEntity::toDomain);
    }
}
