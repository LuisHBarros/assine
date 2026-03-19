package br.com.assine.subscriptions.adapter.in.web;

import br.com.assine.subscriptions.domain.model.Subscription;
import br.com.assine.subscriptions.domain.model.SubscriptionId;
import br.com.assine.subscriptions.domain.model.UserId;
import br.com.assine.subscriptions.domain.port.in.CreateSubscriptionUseCase;
import br.com.assine.subscriptions.domain.port.out.SubscriptionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(CreateSubscriptionUseCase createSubscriptionUseCase, SubscriptionRepository subscriptionRepository) {
        this.createSubscriptionUseCase = createSubscriptionUseCase;
        this.subscriptionRepository = subscriptionRepository;
    }

    public record CreateSubscriptionRequest(
        @NotNull UUID userId,
        @NotNull UUID planId,
        @NotNull String paymentMethod
    ) {}

    public record SubscriptionResponse(
        UUID id,
        UUID userId,
        UUID planId,
        String status,
        String paymentMethod,
        LocalDateTime currentPeriodEnd,
        LocalDateTime createdAt
    ) {
        public static SubscriptionResponse fromDomain(Subscription subscription) {
            return new SubscriptionResponse(
                subscription.getId().value(),
                subscription.getUserId().value(),
                subscription.getPlan().id(),
                subscription.getStatus().name(),
                subscription.getPaymentMethod(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCreatedAt()
            );
        }
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@RequestBody @Valid CreateSubscriptionRequest request) {
        CreateSubscriptionUseCase.Command command = new CreateSubscriptionUseCase.Command(
            request.userId(),
            request.planId(),
            request.paymentMethod()
        );

        Subscription subscription = createSubscriptionUseCase.execute(command);
        return ResponseEntity.ok(SubscriptionResponse.fromDomain(subscription));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable UUID id) {
        return subscriptionRepository.findById(SubscriptionId.fromUUID(id))
            .map(SubscriptionResponse::fromDomain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getByUserId(@PathVariable UUID userId) {
        List<SubscriptionResponse> subscriptions = subscriptionRepository.findByUserId(UserId.fromUUID(userId))
            .stream()
            .map(SubscriptionResponse::fromDomain)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(subscriptions);
    }
}
