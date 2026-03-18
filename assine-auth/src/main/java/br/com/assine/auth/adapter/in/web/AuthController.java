package br.com.assine.auth.adapter.in.web;

import br.com.assine.auth.domain.port.in.OAuthCallbackUseCase;
import br.com.assine.auth.domain.port.in.RequestMagicLinkUseCase;
import br.com.assine.auth.domain.port.in.ValidateMagicLinkUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RequestMagicLinkUseCase requestMagicLinkUseCase;
    private final ValidateMagicLinkUseCase validateMagicLinkUseCase;
    private final OAuthCallbackUseCase oAuthCallbackUseCase;

    public AuthController(RequestMagicLinkUseCase requestMagicLinkUseCase,
                          ValidateMagicLinkUseCase validateMagicLinkUseCase,
                          OAuthCallbackUseCase oAuthCallbackUseCase) {
        this.requestMagicLinkUseCase = requestMagicLinkUseCase;
        this.validateMagicLinkUseCase = validateMagicLinkUseCase;
        this.oAuthCallbackUseCase = oAuthCallbackUseCase;
    }

    @PostMapping("/magic-link")
    public ResponseEntity<Void> requestMagicLink(@RequestBody @Valid MagicLinkRequest request) {
        requestMagicLinkUseCase.request(request.email());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/magic-link/validate")
    public ResponseEntity<AuthResponse> validateMagicLink(@RequestParam("token") String token) {
        String jwt = validateMagicLinkUseCase.validate(token);
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    // Note: This is a simplified callback for the demonstration of the Hexagonal architecture.
    // In a real Spring Security OAuth2 setup, this might be handled by a custom AuthenticationSuccessHandler
    // calling the use case, but we implement it as a controller to show the flow.
    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<AuthResponse> googleCallback(@RequestParam("code") String code) {
        String jwt = oAuthCallbackUseCase.processCallback(code);
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    public record MagicLinkRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}

    public record AuthResponse(String accessToken) {}
}
