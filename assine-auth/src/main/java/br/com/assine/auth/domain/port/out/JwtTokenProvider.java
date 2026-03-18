package br.com.assine.auth.domain.port.out;

import br.com.assine.auth.domain.model.AuthUser;

public interface JwtTokenProvider {
    String generateToken(AuthUser authUser);
}
