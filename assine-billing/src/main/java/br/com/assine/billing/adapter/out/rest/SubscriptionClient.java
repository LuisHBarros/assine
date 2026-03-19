package br.com.assine.billing.adapter.out.rest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(name = "subscriptions-service", url = "${assine.services.subscriptions.url}")
public interface SubscriptionClient {

    @GetMapping("/api/v1/subscriptions/{id}/activation-date")
    LocalDate getActivationDate(@PathVariable("id") UUID subscriptionId);
}
