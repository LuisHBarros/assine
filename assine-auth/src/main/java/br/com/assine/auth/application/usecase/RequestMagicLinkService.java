package br.com.assine.auth.application.usecase;

import br.com.assine.auth.domain.model.MagicToken;
import br.com.assine.auth.domain.port.in.RequestMagicLinkUseCase;
import br.com.assine.auth.domain.port.out.MagicLinkEventPublisher;
import br.com.assine.auth.domain.port.out.MagicTokenRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RequestMagicLinkService implements RequestMagicLinkUseCase {

    private final MagicTokenRepository magicTokenRepository;
    private final MagicLinkEventPublisher eventPublisher;

    public RequestMagicLinkService(MagicTokenRepository magicTokenRepository, MagicLinkEventPublisher eventPublisher) {
        this.magicTokenRepository = magicTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void request(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        UUID id = UuidCreator.getTimeOrderedEpoch();
        String tokenString = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        MagicToken magicToken = new MagicToken(id, email, tokenString, expiresAt, false);
        magicTokenRepository.save(magicToken);

        eventPublisher.publishRequestedEvent(magicToken.email(), magicToken.token());
    }
}
