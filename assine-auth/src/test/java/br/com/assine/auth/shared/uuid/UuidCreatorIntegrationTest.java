package br.com.assine.auth.shared.uuid;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidCreatorIntegrationTest {

    @Test
    void shouldGenerateVersion7UuidWithRfcVariant() {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void shouldKeepGenerationOrderedWithinTheSameMillisecond() {
        List<UUID> generated = new ArrayList<>();
        for (int index = 0; index < 64; index++) {
            generated.add(UuidCreator.getTimeOrderedEpoch());
        }

        assertThat(generated)
                .isSortedAccordingTo(UUID::compareTo)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldEmbedUnixTimestampForGivenInstant() {
        Instant instant = Instant.parse("2026-03-17T12:00:00Z");

        UUID uuid = UuidCreator.getTimeOrderedEpoch(instant);

        assertThat(extractUnixTimestamp(uuid)).isEqualTo(instant.toEpochMilli());
    }

    private static long extractUnixTimestamp(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }
}
