package br.com.assine.auth.domain.port.out;

public interface MagicLinkEventPublisher {
    void publishRequestedEvent(String email, String token);
}
