package br.com.assine.subscriptions.domain.port.in;

import java.util.UUID;

public interface CancelSubscriptionUseCase {
    void execute(UUID subscriptionId);
}
