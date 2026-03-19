package br.com.assine.subscriptions.application.usecase;

import br.com.assine.subscriptions.domain.event.SubscriptionActivated;
import br.com.assine.subscriptions.domain.model.Subscription;
import br.com.assine.subscriptions.domain.model.SubscriptionId;
import br.com.assine.subscriptions.domain.port.in.ActivateSubscriptionUseCase;
import br.com.assine.subscriptions.domain.port.out.DomainEventPublisher;
import br.com.assine.subscriptions.domain.port.out.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivateSubscriptionService implements ActivateSubscriptionUseCase {
    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;

    public ActivateSubscriptionService(SubscriptionRepository subscriptionRepository, DomainEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void execute(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(SubscriptionId.fromUUID(subscriptionId))
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        subscription.activate();
        subscriptionRepository.save(subscription);

        eventPublisher.publish(new SubscriptionActivated(
            subscription.getId().value(),
            subscription.getUserId().value(),
            subscription.getCurrentPeriodEnd()
        ));
    }
}
