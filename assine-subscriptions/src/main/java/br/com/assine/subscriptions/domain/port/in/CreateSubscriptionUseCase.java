package br.com.assine.subscriptions.domain.port.in;

import br.com.assine.subscriptions.domain.model.Subscription;
import java.util.UUID;

public interface CreateSubscriptionUseCase {
    Subscription execute(Command command);

    record Command(
        UUID userId,
        UUID planId,
        String paymentMethod
    ) {}
}
