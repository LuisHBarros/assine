package br.com.assine.subscriptions.domain.port.in;

import java.util.UUID;

public interface ActivateSubscriptionUseCase {
    void execute(UUID subscriptionId);
}
