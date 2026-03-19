package br.com.assine.billing.adapter.in.web;

import br.com.assine.billing.domain.port.out.PaymentGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentGateway paymentGateway;

    public PaymentController(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public record SubscriptionRequest(
            UUID subscriptionId,
            String customerEmail,
            String customerName,
            String paymentMethodId,
            int amountCents
    ) {}

    @PostMapping("/subscriptions")
    public ResponseEntity<PaymentGateway.PaymentIntent> createSubscriptionIntent(@RequestBody SubscriptionRequest request) {
        PaymentGateway.CreateSubscriptionCommand command = new PaymentGateway.CreateSubscriptionCommand(
                request.subscriptionId(),
                request.customerEmail(),
                request.customerName(),
                request.paymentMethodId(),
                request.amountCents()
        );
        
        PaymentGateway.PaymentIntent intent = paymentGateway.createSubscription(command);
        return ResponseEntity.ok(intent);
    }
}
