package br.com.assine.auth.adapter.in.web;

import br.com.assine.auth.domain.model.TokenPair;
import br.com.assine.auth.domain.port.in.OAuthCallbackUseCase;
import br.com.assine.auth.domain.port.in.RefreshTokenUseCase;
import br.com.assine.auth.domain.port.in.RequestMagicLinkUseCase;
import br.com.assine.auth.domain.port.in.ValidateMagicLinkUseCase;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestMagicLinkUseCase requestMagicLinkUseCase;

    @MockBean
    private ValidateMagicLinkUseCase validateMagicLinkUseCase;

    @MockBean
    private OAuthCallbackUseCase oAuthCallbackUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void postMagicLink_ShouldReturnAccepted() throws Exception {
        String json = "{\"email\": \"user@example.com\"}";

        mockMvc.perform(post("/auth/magic-link")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted());

        verify(requestMagicLinkUseCase).request("user@example.com");
    }

    @Test
    void validateMagicLink_ShouldReturnTokenPair() throws Exception {
        TokenPair tokenPair = new TokenPair("access123", "refresh123");
        when(validateMagicLinkUseCase.validate("token123")).thenReturn(tokenPair);

        mockMvc.perform(get("/auth/magic-link/validate")
                        .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh123"));
    }

    @Test
    void googleCallback_ShouldReturnTokenPair() throws Exception {
        TokenPair tokenPair = new TokenPair("ga123", "gr123");
        when(oAuthCallbackUseCase.processCallback("code123")).thenReturn(tokenPair);

        mockMvc.perform(get("/auth/oauth2/google/callback")
                        .param("code", "code123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("ga123"))
                .andExpect(jsonPath("$.refreshToken").value("gr123"));
    }

    @Test
    void refresh_ShouldReturnNewTokenPair() throws Exception {
        TokenPair tokenPair = new TokenPair("newAccess", "newRefresh");
        when(refreshTokenUseCase.refresh("oldRefresh")).thenReturn(tokenPair);

        String json = "{\"refreshToken\": \"oldRefresh\"}";

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccess"))
                .andExpect(jsonPath("$.refreshToken").value("newRefresh"));
    }

    @Test
    void getJwks_ShouldReturnJwks() throws Exception {
        Map<String, Object> jwks = Map.of("keys", "[...]");
        when(jwtTokenProvider.getJwks()).thenReturn(jwks);

        mockMvc.perform(get("/auth/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").exists());
    }
}
