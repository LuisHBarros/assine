package br.com.assine.subscriptions.application.usecase;

import br.com.assine.subscriptions.domain.model.Plan;
import br.com.assine.subscriptions.domain.model.Subscription;
import br.com.assine.subscriptions.domain.model.UserId;
import br.com.assine.subscriptions.domain.port.in.CreateSubscriptionUseCase;
import br.com.assine.subscriptions.domain.port.out.PlanRepository;
import br.com.assine.subscriptions.domain.port.out.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSubscriptionService implements CreateSubscriptionUseCase {
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public CreateSubscriptionService(SubscriptionRepository subscriptionRepository, PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public Subscription execute(Command command) {
        Plan plan = planRepository.findById(command.planId())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + command.planId()));

        if (!plan.active()) {
            throw new IllegalArgumentException("Plan is not active: " + plan.name());
        }

        Subscription subscription = Subscription.create(
            UserId.fromUUID(command.userId()),
            plan,
            command.paymentMethod()
        );

        return subscriptionRepository.save(subscription);
    }
}
