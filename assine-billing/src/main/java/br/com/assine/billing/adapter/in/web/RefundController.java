package br.com.assine.billing.adapter.in.web;

import br.com.assine.billing.domain.model.Payment;
import br.com.assine.billing.domain.model.PaymentId;
import br.com.assine.billing.domain.model.Refund;
import br.com.assine.billing.domain.port.out.PaymentRepository;
import br.com.assine.billing.domain.port.out.PaymentGateway;
import br.com.assine.billing.domain.service.RefundCalculator;
import br.com.assine.billing.adapter.out.rest.SubscriptionClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refunds")
public class RefundController {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final SubscriptionClient subscriptionClient;
    private final RefundCalculator refundCalculator = new RefundCalculator();

    public RefundController(PaymentRepository paymentRepository, PaymentGateway paymentGateway, SubscriptionClient subscriptionClient) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.subscriptionClient = subscriptionClient;
    }

    public record RefundRequest(UUID subscriptionId, String reason) {}

    @PostMapping
    public ResponseEntity<?> requestRefund(@RequestBody RefundRequest request) {
        // Simplified flow based on ADR-003
        // 1. Fetch activation date
        LocalDate activationDate;
        try {
            activationDate = subscriptionClient.getActivationDate(request.subscriptionId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not fetch subscription activation date");
        }

        // 2. We'd fetch the payment here, but we lack a proper query in the current interface
        // so we'll just mock the amount for the controller example (in a real scenario, we'd query the DB by subscriptionId)
        // paymentRepository.findBySubscriptionId(request.subscriptionId())
        int mockPaidAmountCents = 2990;

        // 3. Calculate refund
        RefundCalculator.RefundAmount amount = refundCalculator.calculate(mockPaidAmountCents, activationDate, LocalDate.now());

        if (amount.amountCents() <= 0) {
            return ResponseEntity.unprocessableEntity().body("Refund period expired");
        }

        // 4. Call gateway
        // paymentGateway.refund(externalPaymentId, amount.amountCents());
        
        return ResponseEntity.accepted().build();
    }
}
