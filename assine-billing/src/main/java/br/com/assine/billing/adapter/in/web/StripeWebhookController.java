package br.com.assine.billing.adapter.in.web;

import br.com.assine.billing.domain.port.in.ProcessWebhookUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

    private final ProcessWebhookUseCase processWebhookUseCase;

    public StripeWebhookController(ProcessWebhookUseCase processWebhookUseCase) {
        this.processWebhookUseCase = processWebhookUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        processWebhookUseCase.process(payload, signature);
        return ResponseEntity.ok().build();
    }
}
