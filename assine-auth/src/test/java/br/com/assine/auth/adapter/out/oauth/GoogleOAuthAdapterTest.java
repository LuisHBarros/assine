package br.com.assine.auth.adapter.out.oauth;

import br.com.assine.auth.domain.port.out.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GoogleOAuthAdapter.class)
@TestPropertySource(properties = {
        "google.oauth2.client-id=test-client-id",
        "google.oauth2.client-secret=test-client-secret",
        "google.oauth2.token-uri=https://oauth2.googleapis.com/token",
        "google.oauth2.user-info-uri=https://www.googleapis.com/oauth2/v2/userinfo"
})
class GoogleOAuthAdapterTest {

    @Autowired
    private GoogleOAuthAdapter googleOAuthAdapter;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void getUserInfo_WhenSuccessful_ShouldReturnUserInfo() {
        String code = "valid-code";
        String accessToken = "mock-access-token";
        String email = "user@example.com";
        String sub = "123456789";

        // Mock token exchange
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=valid-code")))
                .andRespond(withSuccess("{\"access_token\":\"" + accessToken + "\"}", MediaType.APPLICATION_JSON));

        // Mock user info fetch
        server.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + accessToken))
                .andRespond(withSuccess("{\"email\":\"" + email + "\", \"id\":\"" + sub + "\"}", MediaType.APPLICATION_JSON));

        Optional<OAuthProvider.OAuthUserInfo> result = googleOAuthAdapter.getUserInfo(code);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo(email);
        assertThat(result.get().providerAccountId()).isEqualTo(sub);
    }

    @Test
    void getUserInfo_WhenTokenFetchFails_ShouldReturnEmpty() {
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON)); // Missing access_token

        Optional<OAuthProvider.OAuthUserInfo> result = googleOAuthAdapter.getUserInfo("invalid-code");

        assertThat(result).isEmpty();
    }
}
