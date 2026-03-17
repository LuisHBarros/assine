package br.com.assine.auth.adapter.out.persistence;

import br.com.assine.auth.domain.model.MagicToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MagicTokenRepositoryAdapter.class)
class MagicTokenRepositoryAdapterTest {

    @Autowired
    private MagicTokenRepositoryAdapter magicTokenRepositoryAdapter;

    @Test
    void shouldAssignUuidV7WhenPersistingMagicToken() {
        MagicToken saved = magicTokenRepositoryAdapter.save(
                new MagicToken(null, "user@example.com", "magic-token", LocalDateTime.now().plusMinutes(15), false)
        );

        assertThat(saved.id()).isNotNull();
        assertThat(saved.id().version()).isEqualTo(7);
    }

    @Test
    void shouldFindMagicTokenByToken() {
        magicTokenRepositoryAdapter.save(
                new MagicToken(null, "user@example.com", "search-token", LocalDateTime.now().plusMinutes(15), false)
        );

        assertThat(magicTokenRepositoryAdapter.findByToken("search-token"))
                .isPresent()
                .get()
                .extracting(MagicToken::token)
                .isEqualTo("search-token");
    }
}
