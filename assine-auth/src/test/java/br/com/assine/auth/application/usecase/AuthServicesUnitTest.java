package br.com.assine.auth.application.usecase;

import br.com.assine.auth.domain.model.AuthProvider;
import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.MagicToken;
import br.com.assine.auth.domain.port.out.AuthUserRepository;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import br.com.assine.auth.domain.port.out.MagicLinkEventPublisher;
import br.com.assine.auth.domain.port.out.MagicTokenRepository;
import br.com.assine.auth.domain.port.out.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServicesUnitTest {

    @Mock private AuthUserRepository authUserRepository;
    @Mock private MagicTokenRepository magicTokenRepository;
    @Mock private MagicLinkEventPublisher eventPublisher;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private OAuthProvider oAuthProvider;

    private RequestMagicLinkService requestService;
    private ValidateMagicLinkService validateService;
    private OAuthCallbackService oAuthService;

    @BeforeEach
    void setUp() {
        requestService = new RequestMagicLinkService(magicTokenRepository, eventPublisher);
        validateService = new ValidateMagicLinkService(magicTokenRepository, authUserRepository, jwtTokenProvider);
        oAuthService = new OAuthCallbackService(authUserRepository, jwtTokenProvider, oAuthProvider);
    }

    @Test
    void requestMagicLink_ShouldSaveTokenAndPublishEvent() {
        String email = "test@example.com";

        requestService.request(email);

        verify(magicTokenRepository).save(any(MagicToken.class));
        verify(eventPublisher).publishRequestedEvent(eq(email), anyString());
    }

    @Test
    void validateMagicLink_WhenValid_ShouldReturnJwtAndLinkAccount() {
        String token = "valid-token";
        String email = "test@example.com";
        MagicToken magicToken = new MagicToken(UUID.randomUUID(), email, token, LocalDateTime.now().plusMinutes(10), false);
        
        when(magicTokenRepository.findByToken(token)).thenReturn(Optional.of(magicToken));
        when(authUserRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateToken(any(AuthUser.class))).thenReturn("mock-jwt");

        String result = validateService.validate(token);

        assertThat(result).isEqualTo("mock-jwt");
        verify(magicTokenRepository).save(argThat(MagicToken::used));
        
        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().email()).isEqualTo(email);
        assertThat(userCaptor.getValue().provider()).isEqualTo(AuthProvider.MAGIC_LINK);
    }

    @Test
    void validateMagicLink_WhenExpired_ShouldThrowException() {
        String token = "expired-token";
        MagicToken magicToken = new MagicToken(UUID.randomUUID(), "test@example.com", token, LocalDateTime.now().minusMinutes(1), false);
        
        when(magicTokenRepository.findByToken(token)).thenReturn(Optional.of(magicToken));

        assertThatThrownBy(() -> validateService.validate(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void processOAuthCallback_ShouldLinkExistingAccount() {
        String code = "valid-code";
        String email = "existing@example.com";
        OAuthProvider.OAuthUserInfo userInfo = new OAuthProvider.OAuthUserInfo(email, "sub123");
        AuthUser existingUser = new AuthUser(null, email, AuthProvider.MAGIC_LINK, null, null);
        
        when(oAuthProvider.getUserInfo(code)).thenReturn(Optional.of(userInfo));
        when(authUserRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateToken(any(AuthUser.class))).thenReturn("oauth-jwt");

        String result = oAuthService.processCallback(code);

        assertThat(result).isEqualTo("oauth-jwt");
        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(userCaptor.capture());
        
        // Should keep original provider but update login time
        assertThat(userCaptor.getValue().provider()).isEqualTo(AuthProvider.MAGIC_LINK);
        assertThat(userCaptor.getValue().lastLogin()).isNotNull();
    }
}
