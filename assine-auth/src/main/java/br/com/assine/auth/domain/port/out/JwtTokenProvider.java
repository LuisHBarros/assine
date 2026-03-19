package br.com.assine.auth.domain.port.out;

import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.TokenPair;

import java.util.Map;

public interface JwtTokenProvider {
    TokenPair generateTokenPair(AuthUser authUser);
    String validateTokenAndGetEmail(String token);
    Map<String, Object> getJwks();
}
