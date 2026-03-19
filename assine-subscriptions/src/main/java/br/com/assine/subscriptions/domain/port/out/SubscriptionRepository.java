package br.com.assine.subscriptions.domain.port.out;

import br.com.assine.subscriptions.domain.model.Subscription;
import br.com.assine.subscriptions.domain.model.SubscriptionId;
import br.com.assine.subscriptions.domain.model.UserId;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(SubscriptionId id);
    List<Subscription> findByUserId(UserId userId);
}
