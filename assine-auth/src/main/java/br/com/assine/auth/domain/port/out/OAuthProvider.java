package br.com.assine.auth.domain.port.out;

import java.util.Optional;

public interface OAuthProvider {
    Optional<OAuthUserInfo> getUserInfo(String code);

    record OAuthUserInfo(String email, String providerAccountId) {}
}
