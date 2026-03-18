package br.com.assine.auth.domain.port.in;

public interface OAuthCallbackUseCase {
    String processCallback(String code);
}
