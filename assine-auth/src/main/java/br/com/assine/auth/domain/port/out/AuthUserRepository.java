package br.com.assine.auth.domain.port.out;

import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.AuthUserId;

import java.util.Optional;

public interface AuthUserRepository {

    Optional<AuthUser> findById(AuthUserId id);

    Optional<AuthUser> findByEmail(String email);

    AuthUser save(AuthUser authUser);
}
