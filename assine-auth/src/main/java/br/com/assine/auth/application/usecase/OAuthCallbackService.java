package br.com.assine.auth.application.usecase;

import br.com.assine.auth.domain.model.AuthProvider;
import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.AuthUserId;
import br.com.assine.auth.domain.model.UserRole;
import br.com.assine.auth.domain.port.in.OAuthCallbackUseCase;
import br.com.assine.auth.domain.port.out.AuthUserRepository;
import br.com.assine.auth.domain.port.out.JwtTokenProvider;
import br.com.assine.auth.domain.port.out.OAuthProvider;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OAuthCallbackService implements OAuthCallbackUseCase {

    private final AuthUserRepository authUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthProvider oAuthProvider;

    public OAuthCallbackService(AuthUserRepository authUserRepository, 
                                JwtTokenProvider jwtTokenProvider,
                                OAuthProvider oAuthProvider) {
        this.authUserRepository = authUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.oAuthProvider = oAuthProvider;
    }

    @Override
    @Transactional
    public String processCallback(String code) {
        OAuthProvider.OAuthUserInfo userInfo = oAuthProvider.getUserInfo(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid OAuth code or failed to fetch user info"));

        String email = userInfo.email();

        AuthUser authUser = authUserRepository.findByEmail(email)
                .map(existingUser -> new AuthUser(
                        existingUser.id(),
                        existingUser.email(),
                        existingUser.provider(),
                        existingUser.role(),
                        LocalDateTime.now()
                ))
                .orElseGet(() -> new AuthUser(
                        new AuthUserId(UuidCreator.getTimeOrderedEpoch()),
                        email,
                        AuthProvider.GOOGLE,
                        UserRole.USER,
                        LocalDateTime.now()
                ));

        AuthUser savedUser = authUserRepository.save(authUser);

        return jwtTokenProvider.generateToken(savedUser);
    }
}
