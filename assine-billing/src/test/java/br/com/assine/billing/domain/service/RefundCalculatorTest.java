package br.com.assine.billing.domain.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator();

    @Test
    void shouldReturnFullRefund_whenWithin14Days() {
        LocalDate activatedAt = LocalDate.of(2024, 1, 1);
        LocalDate requestedAt = LocalDate.of(2024, 1, 15); // Exactly 14 days later

        RefundCalculator.RefundAmount result = calculator.calculate(2990, activatedAt, requestedAt);

        assertEquals(2990, result.amountCents());
        assertEquals(100.0, result.percentage());
        assertEquals(14, result.daysSinceActivation());
    }

    @Test
    void shouldReturnFullRefund_whenRequestedAtDay15() {
        LocalDate activatedAt = LocalDate.of(2024, 1, 1);
        LocalDate requestedAt = LocalDate.of(2024, 1, 16); // Day 15

        RefundCalculator.RefundAmount result = calculator.calculate(2990, activatedAt, requestedAt);

        // Based on ADR: "Day 15: refund of 100% (15/15) — last full day"
        // ChronoUnit.DAYS.between(1, 16) is 15.
        assertEquals(2990, result.amountCents());
        assertEquals(100.0, result.percentage());
        assertEquals(15, result.daysSinceActivation());
    }

    @Test
    void shouldReturnPartialRefund_whenAtDay16() {
        LocalDate activatedAt = LocalDate.of(2024, 1, 1);
        LocalDate requestedAt = LocalDate.of(2024, 1, 17); // Day 16

        RefundCalculator.RefundAmount result = calculator.calculate(2990, activatedAt, requestedAt);

        // Formula: (30 - 16) / 15 = 14 / 15 = 0.9333...
        // 2990 * 0.9333 = 2790.6 -> floor 2790
        assertEquals(2790, result.amountCents());
        assertTrue(result.percentage() < 100.0);
        assertEquals(16, result.daysSinceActivation());
    }

    @Test
    void shouldReturnPartialRefund_whenAtDay20() {
        LocalDate activatedAt = LocalDate.of(2024, 1, 1);
        LocalDate requestedAt = LocalDate.of(2024, 1, 21); // Day 20

        RefundCalculator.RefundAmount result = calculator.calculate(2990, activatedAt, requestedAt);

        // Formula: (30 - 20) / 15 = 10 / 15 = 0.666...
        // 2990 * 0.666 = 1993.3 -> floor 1993
        assertEquals(1993, result.amountCents());
        assertEquals(20, result.daysSinceActivation());
    }

    @Test
    void shouldReturnPartialRefund_whenAtDay25() {
        LocalDate activatedAt = LocalDate.of(2024, 1, 1);
        LocalDate requestedAt = LocalDate.of(2024, 1, 26); // Day 25

        RefundCalculator.RefundAmount result = calculator.calculate(2990, activatedAt, requestedAt);

        // Formula: (30 - 25) / 15 = 5 / 15 = 0.333...
        // 2990 * 0.333 = 996.6 -> floor 996
        assertEquals(996, result.amountCents());
        assertEquals(25, result.daysSinceActivation());
    }

    @Test
    void shouldReturnZero_whenAtDay30() {
        LocalDate activatedAt = LocalDate.of(2024, 1, 1);
        LocalDate requestedAt = LocalDate.of(2024, 1, 31); // Day 30

        RefundCalculator.RefundAmount result = calculator.calculate(2990, activatedAt, requestedAt);

        assertEquals(0, result.amountCents());
        assertEquals(0.0, result.percentage());
        assertEquals(30, result.daysSinceActivation());
    }
}
