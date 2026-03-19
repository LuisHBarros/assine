package br.com.assine.access.application.usecase;

import br.com.assine.access.domain.model.AccessPermission;
import br.com.assine.access.domain.port.in.GrantAccessUseCase;
import br.com.assine.access.domain.port.out.AccessPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrantAccessServiceTest {

    @Mock
    private AccessPermissionRepository repository;

    private GrantAccessService grantAccessService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        grantAccessService = new GrantAccessService(repository);
    }

    @Test
    void shouldGrantAccessSuccessfully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusMonths(1);
        
        GrantAccessUseCase.Command command = new GrantAccessUseCase.Command(
            userId, "newsletter", subscriptionId, expiresAt
        );

        // Act
        grantAccessService.execute(command);

        // Assert
        ArgumentCaptor<AccessPermission> captor = ArgumentCaptor.forClass(AccessPermission.class);
        verify(repository, times(1)).save(captor.capture());

        AccessPermission savedPermission = captor.getValue();
        assertEquals(userId, savedPermission.getUserId());
        assertEquals("newsletter", savedPermission.getResource());
        assertEquals(subscriptionId, savedPermission.getSubscriptionId());
        assertEquals(expiresAt, savedPermission.getExpiresAt());
        assertTrue(savedPermission.isActive());
    }
}
