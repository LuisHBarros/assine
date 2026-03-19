package br.com.assine.auth.domain.port.in;

import br.com.assine.auth.domain.model.TokenPair;

public interface OAuthCallbackUseCase {
    TokenPair processCallback(String code);
}
