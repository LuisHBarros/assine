package br.com.assine.subscriptions.adapter.out.persistence;

import br.com.assine.subscriptions.domain.model.Subscription;
import br.com.assine.subscriptions.domain.model.SubscriptionId;
import br.com.assine.subscriptions.domain.model.UserId;
import br.com.assine.subscriptions.domain.port.out.SubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository subscriptionJpaRepository;

    public SubscriptionPersistenceAdapter(SubscriptionJpaRepository subscriptionJpaRepository) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = SubscriptionEntity.fromDomain(subscription);
        SubscriptionEntity savedEntity = subscriptionJpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return subscriptionJpaRepository.findById(id.value())
            .map(SubscriptionEntity::toDomain);
    }

    @Override
    public List<Subscription> findByUserId(UserId userId) {
        return subscriptionJpaRepository.findByUserId(userId.value())
            .stream()
            .map(SubscriptionEntity::toDomain)
            .collect(Collectors.toList());
    }
}
