package br.com.assine.billing.domain.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RefundCalculator {

    public RefundAmount calculate(
        int paidAmountCents,
        LocalDate activatedAt,
        LocalDate refundRequestedAt
    ) {
        long daysSinceActivation = ChronoUnit.DAYS.between(activatedAt, refundRequestedAt);

        if (daysSinceActivation <= 14) {
            return new RefundAmount(paidAmountCents, 100.0, (int) daysSinceActivation);
        }

        if (daysSinceActivation >= 30) {
            return RefundAmount.zero((int) daysSinceActivation);
        }

        // Formula: 15 <= daysSinceActivation <= 29
        // percentual = (30 - daysSinceActivation) / 15
        long daysRemaining = 30 - daysSinceActivation;
        double percentage = (double) daysRemaining / 15.0;
        int refundAmountCents = (int) Math.floor(paidAmountCents * percentage);

        return new RefundAmount(refundAmountCents, percentage * 100.0, (int) daysSinceActivation);
    }

    public record RefundAmount(
        int amountCents,
        double percentage,
        int daysSinceActivation
    ) {
        public static RefundAmount zero(int daysSinceActivation) {
            return new RefundAmount(0, 0.0, daysSinceActivation);
        }
    }
}
