package br.com.assine.auth.adapter.out.persistence;

import br.com.assine.auth.domain.model.AuthProvider;
import br.com.assine.auth.domain.model.AuthUser;
import br.com.assine.auth.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AuthUserRepositoryAdapter.class)
class AuthUserRepositoryAdapterTest {

    @Autowired
    private AuthUserRepositoryAdapter authUserRepositoryAdapter;

    @Test
    void shouldAssignUuidV7WhenPersistingAuthUser() {
        AuthUser saved = authUserRepositoryAdapter.save(
                new AuthUser(null, "User@Example.com", AuthProvider.MAGIC_LINK, UserRole.USER, null)
        );

        assertThat(saved.id()).isNotNull();
        assertThat(saved.id().value().version()).isEqualTo(7);
        assertThat(saved.email()).isEqualTo("user@example.com");
    }

    @Test
    void shouldFindAuthUserByEmail() {
        authUserRepositoryAdapter.save(
                new AuthUser(null, "admin@example.com", AuthProvider.GOOGLE, UserRole.ADMIN, null)
        );

        assertThat(authUserRepositoryAdapter.findByEmail("admin@example.com"))
                .isPresent()
                .get()
                .extracting(AuthUser::role)
                .isEqualTo(UserRole.ADMIN);
    }
}
