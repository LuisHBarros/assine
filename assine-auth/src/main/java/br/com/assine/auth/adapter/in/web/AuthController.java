package br.com.assine.auth.adapter.in.web;

import br.com.assine.auth.domain.model.TokenPair;
import br.com.assine.auth.domain.port.in.OAuthCallbackUseCase;
import br.com.assine.auth.domain.port.in.RefreshTokenUseCase;
import br.com.assine.auth.domain.port.in.RequestMagicLinkUseCase;
import br.com.assine.auth.domain.port.in.ValidateMagicLinkUseCase;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RequestMagicLinkUseCase requestMagicLinkUseCase;
    private final ValidateMagicLinkUseCase validateMagicLinkUseCase;
    private final OAuthCallbackUseCase oAuthCallbackUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(RequestMagicLinkUseCase requestMagicLinkUseCase,
                          ValidateMagicLinkUseCase validateMagicLinkUseCase,
                          OAuthCallbackUseCase oAuthCallbackUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          JwtTokenProvider jwtTokenProvider) {
        this.requestMagicLinkUseCase = requestMagicLinkUseCase;
        this.validateMagicLinkUseCase = validateMagicLinkUseCase;
        this.oAuthCallbackUseCase = oAuthCallbackUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/magic-link")
    public ResponseEntity<Void> requestMagicLink(@RequestBody @Valid MagicLinkRequest request) {
        requestMagicLinkUseCase.request(request.email());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/magic-link/validate")
    public ResponseEntity<AuthResponse> validateMagicLink(@RequestParam("token") String token) {
        TokenPair tokenPair = validateMagicLinkUseCase.validate(token);
        return ResponseEntity.ok(AuthResponse.fromDomain(tokenPair));
    }

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<AuthResponse> googleCallback(@RequestParam("code") String code) {
        TokenPair tokenPair = oAuthCallbackUseCase.processCallback(code);
        return ResponseEntity.ok(AuthResponse.fromDomain(tokenPair));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        TokenPair tokenPair = refreshTokenUseCase.refresh(request.refreshToken());
        return ResponseEntity.ok(AuthResponse.fromDomain(tokenPair));
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        return jwtTokenProvider.getJwks();
    }

    public record MagicLinkRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record AuthResponse(String accessToken, String refreshToken) {
        public static AuthResponse fromDomain(TokenPair tokenPair) {
            return new AuthResponse(tokenPair.accessToken(), tokenPair.refreshToken());
        }
    }
}
