package br.com.assine.auth.adapter.out.oauth;

import br.com.assine.auth.domain.port.out.OAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class GoogleOAuthAdapter implements OAuthProvider {

    private final RestTemplate restTemplate;

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    @Value("${google.oauth2.token-uri}")
    private String tokenUri;

    @Value("${google.oauth2.user-info-uri}")
    private String userInfoUri;

    // Use a fixed redirect URI for the demonstration (it should match the one configured in Google Cloud Console)
    private final String redirectUri = "https://assine.com/auth/oauth2/google/callback";

    public GoogleOAuthAdapter(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<OAuthUserInfo> getUserInfo(String code) {
        try {
            // 1. Exchange code for access token
            String accessToken = fetchAccessToken(code);

            // 2. Fetch user info using access token
            return fetchUserInfo(accessToken);

        } catch (Exception e) {
            // LOG error (in a real app)
            return Optional.empty();
        }
    }

    private String fetchAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String token = (String) response.getBody().get("access_token");
            if (token != null && !token.isBlank()) {
                return token;
            }
        }

        throw new RuntimeException("Failed to fetch access token from Google");
    }

    private Optional<OAuthUserInfo> fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String email = (String) response.getBody().get("email");
            String sub = (String) response.getBody().get("id"); // or sub, depending on provider

            if (email != null && sub != null) {
                return Optional.of(new OAuthUserInfo(email, sub));
            }
        }

        return Optional.empty();
    }
}
