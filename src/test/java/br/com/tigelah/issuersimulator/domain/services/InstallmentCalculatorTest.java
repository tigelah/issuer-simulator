package br.com.tigelah.issuersimulator.domain.services;

import br.com.tigelah.issuersimulator.domain.model.InstallmentBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InstallmentCalculatorTest {

    @Test
    void defaults_to_1_when_installments_is_zero_or_negative() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of("*", Map.of(2, new BigDecimal("0.10")))
        );

        InstallmentBreakdown b1 = calc.calculate("m1", 1000, 0);
        assertEquals(1, b1.installments());
        assertEquals(0, b1.interestCents());
        assertEquals(1000, b1.totalCents());
        assertEquals(1000, b1.installmentAmountCents());

        InstallmentBreakdown b2 = calc.calculate("m1", 1000, -10);
        assertEquals(1, b2.installments());
        assertEquals(0, b2.interestCents());
        assertEquals(1000, b2.totalCents());
        assertEquals(1000, b2.installmentAmountCents());
    }

    @Test
    void throws_invalid_installments_when_not_allowed_plan() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of("*", Map.of(2, new BigDecimal("0.02")))
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> calc.calculate("m1", 1000, 5));
        assertEquals("invalid_installments", ex.getMessage());
    }

    @Test
    void cash_purchase_installments_1_has_no_interest() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of("*", Map.of(2, new BigDecimal("0.02")))
        );

        var b = calc.calculate("m1", 1234, 1);

        assertEquals(1, b.installments());
        assertEquals(1234, b.principalCents());
        assertEquals(0, b.interestCents());
        assertEquals(1234, b.totalCents());
        assertEquals(1234, b.installmentAmountCents());
    }

    @Test
    void uses_merchant_specific_rate_when_present() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of(
                        "m1", Map.of(12, new BigDecimal("0.10")),
                        "*", Map.of(12, new BigDecimal("0.12"))
                )
        );

        var b = calc.calculate("m1", 1000, 12);

        assertEquals(12, b.installments());
        assertEquals(1000, b.principalCents());
        assertEquals(100, b.interestCents());
        assertEquals(1100, b.totalCents());
        assertEquals(92, b.installmentAmountCents()); // ceil
    }

    @Test
    void uses_fallback_rate_when_merchant_not_present() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of(
                        "*", Map.of(6, new BigDecimal("0.06")) // 6%
                )
        );

        var b = calc.calculate("unknown-merchant", 1000, 6);

        assertEquals(6, b.installments());
        assertEquals(60, b.interestCents());
        assertEquals(1060, b.totalCents());
        assertEquals(177, b.installmentAmountCents());
    }

    @Test
    void throws_installments_not_supported_when_no_rates_for_merchant_and_no_fallback() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of(
                        "m1", Map.of(2, new BigDecimal("0.02"))
                )
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> calc.calculate("m2", 1000, 2));
        assertEquals("installments_not_supported", ex.getMessage());
    }

    @Test
    void throws_installments_not_supported_when_rate_missing_for_plan() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of(
                        "*", Map.of(2, new BigDecimal("0.02"))
                )
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> calc.calculate("m1", 1000, 6));
        assertEquals("installments_not_supported", ex.getMessage());
    }

    @Test
    void interest_uses_half_up_rounding() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of("*", Map.of(2, new BigDecimal("0.015")))
        );

        var b = calc.calculate("m1", 333, 2);

        assertEquals(5, b.interestCents());
        assertEquals(338, b.totalCents());
        assertEquals(169, b.installmentAmountCents());
    }

    @Test
    void installment_amount_is_ceiling_of_total_divided_by_installments() {
        var calc = new InstallmentCalculator(
                Set.of(1, 2, 6, 12),
                Map.of("*", Map.of(2, new BigDecimal("0.00")))
        );

        var b = calc.calculate("m1", 1001, 2);

        assertEquals(1001, b.totalCents());
        assertEquals(501, b.installmentAmountCents());
    }
}
