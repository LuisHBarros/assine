package br.com.assine.auth.domain.port.in;

public interface RequestMagicLinkUseCase {
    void request(String email);
}
