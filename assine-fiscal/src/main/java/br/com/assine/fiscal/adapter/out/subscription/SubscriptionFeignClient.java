package br.com.assine.fiscal.adapter.out.subscription;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "assine-subscriptions", url = "${subscription.service.url:http://assine-subscriptions:8081}")
public interface SubscriptionFeignClient {

    @GetMapping("/api/v1/subscriptions/{subscriptionId}/payer-data")
    PayerDataResponse getPayerData(@PathVariable("subscriptionId") UUID subscriptionId);

    record PayerDataResponse(
        String name,
        String taxId,
        String email,
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state
    ) {}
}
