package br.com.assine.auth.domain.port.in;

public interface ValidateMagicLinkUseCase {
    String validate(String token);
}
