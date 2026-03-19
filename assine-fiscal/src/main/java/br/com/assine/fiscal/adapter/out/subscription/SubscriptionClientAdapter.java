package br.com.assine.fiscal.adapter.out.subscription;

import br.com.assine.fiscal.domain.port.out.FiscalGateway;
import br.com.assine.fiscal.domain.port.out.SubscriptionGateway;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionClientAdapter implements SubscriptionGateway {

    private final SubscriptionFeignClient feignClient;

    public SubscriptionClientAdapter(SubscriptionFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    public FiscalGateway.PayerData fetchPayerData(UUID subscriptionId) {
        SubscriptionFeignClient.PayerDataResponse response = feignClient.getPayerData(subscriptionId);
        return new FiscalGateway.PayerData(
            response.name(),
            response.taxId(),
            response.email(),
            response.zipCode(),
            response.street(),
            response.number(),
            response.complement(),
            response.neighborhood(),
            response.city(),
            response.state()
        );
    }
}
