package br.com.assine.auth.application.usecase;

import br.com.assine.auth.domain.model.AuthProvider;
import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.AuthUserId;
import br.com.assine.auth.domain.model.MagicToken;
import br.com.assine.auth.domain.model.TokenPair;
import br.com.assine.auth.domain.model.UserRole;
import br.com.assine.auth.domain.port.in.ValidateMagicLinkUseCase;
import br.com.assine.auth.domain.port.out.AuthUserRepository;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import br.com.assine.auth.domain.port.out.MagicTokenRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ValidateMagicLinkService implements ValidateMagicLinkUseCase {

    private final MagicTokenRepository magicTokenRepository;
    private final AuthUserRepository authUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public ValidateMagicLinkService(MagicTokenRepository magicTokenRepository,
                                    AuthUserRepository authUserRepository,
                                    JwtTokenProvider jwtTokenProvider) {
        this.magicTokenRepository = magicTokenRepository;
        this.authUserRepository = authUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public TokenPair validate(String token) {
        MagicToken magicToken = magicTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (magicToken.used()) {
            throw new IllegalStateException("Token already used");
        }

        if (magicToken.expiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }

        // Mark as used
        MagicToken usedToken = new MagicToken(
                magicToken.id(),
                magicToken.email(),
                magicToken.token(),
                magicToken.expiresAt(),
                true
        );
        magicTokenRepository.save(usedToken);

        // Account linking / Creation
        AuthUser authUser = authUserRepository.findByEmail(magicToken.email())
                .map(existingUser -> new AuthUser(
                        existingUser.id(),
                        existingUser.email(),
                        existingUser.provider(),
                        existingUser.role(),
                        LocalDateTime.now()
                ))
                .orElseGet(() -> new AuthUser(
                        new AuthUserId(UuidCreator.getTimeOrderedEpoch()),
                        magicToken.email(),
                        AuthProvider.MAGIC_LINK,
                        UserRole.USER,
                        LocalDateTime.now()
                ));

        AuthUser savedUser = authUserRepository.save(authUser);

        return jwtTokenProvider.generateTokenPair(savedUser);
    }
}
