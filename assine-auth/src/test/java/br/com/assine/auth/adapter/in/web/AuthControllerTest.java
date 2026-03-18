package br.com.assine.auth.adapter.in.web;

import br.com.assine.auth.domain.port.in.OAuthCallbackUseCase;
import br.com.assine.auth.domain.port.in.RequestMagicLinkUseCase;
import br.com.assine.auth.domain.port.in.ValidateMagicLinkUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    void validateMagicLink_ShouldReturnToken() throws Exception {
        when(validateMagicLinkUseCase.validate("token123")).thenReturn("jwt.token.here");

        mockMvc.perform(get("/auth/magic-link/validate")
                        .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token.here"));
    }

    @Test
    void googleCallback_ShouldReturnToken() throws Exception {
        when(oAuthCallbackUseCase.processCallback("code123")).thenReturn("jwt.google.token");

        mockMvc.perform(get("/auth/oauth2/google/callback")
                        .param("code", "code123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.google.token"));
    }
}
