package br.com.assine.auth.application.usecase;

import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.TokenPair;
import br.com.assine.auth.domain.port.in.RefreshTokenUseCase;
import br.com.assine.auth.domain.port.out.AuthUserRepository;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final AuthUserRepository authUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenService(AuthUserRepository authUserRepository, JwtTokenProvider jwtTokenProvider) {
        this.authUserRepository = authUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public TokenPair refresh(String refreshToken) {
        String email = jwtTokenProvider.validateTokenAndGetEmail(refreshToken);
        
        AuthUser authUser = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for refresh token"));

        AuthUser updatedUser = new AuthUser(
                authUser.id(),
                authUser.email(),
                authUser.provider(),
                authUser.role(),
                LocalDateTime.now()
        );
        
        authUserRepository.save(updatedUser);
        
        return jwtTokenProvider.generateTokenPair(updatedUser);
    }
}
