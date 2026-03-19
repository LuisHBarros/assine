package br.com.assine.subscriptions.application.usecase;

import br.com.assine.subscriptions.domain.model.*;
import br.com.assine.subscriptions.domain.port.in.CreateSubscriptionUseCase;
import br.com.assine.subscriptions.domain.port.out.PlanRepository;
import br.com.assine.subscriptions.domain.port.out.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateSubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    private CreateSubscriptionService createSubscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        createSubscriptionService = new CreateSubscriptionService(subscriptionRepository, planRepository);
    }

    @Test
    void shouldCreateSubscriptionSuccessfully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Plan plan = new Plan(planId, "Mensal", 2990, "MONTHLY", true);
        
        CreateSubscriptionUseCase.Command command = new CreateSubscriptionUseCase.Command(
            userId, planId, "CREDIT_CARD"
        );

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Subscription result = createSubscriptionService.execute(command);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId().value());
        assertEquals(planId, result.getPlan().id());
        assertEquals(SubscriptionStatus.PENDING, result.getStatus());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void shouldThrowExceptionWhenPlanNotFound() {
        // Arrange
        UUID planId = UUID.randomUUID();
        CreateSubscriptionUseCase.Command command = new CreateSubscriptionUseCase.Command(
            UUID.randomUUID(), planId, "CREDIT_CARD"
        );

        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> createSubscriptionService.execute(command));
    }
}
