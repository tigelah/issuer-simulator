package br.com.tigelah.issuersimulator.application.usecase;

import br.com.tigelah.issuersimulator.domain.model.InstallmentBreakdown;
import br.com.tigelah.issuersimulator.domain.services.InstallmentCalculator;

public class CalculateInstallmentsUseCase {
    private final InstallmentCalculator calculator;

    public CalculateInstallmentsUseCase(InstallmentCalculator calculator) {
        this.calculator = calculator;
    }

    public InstallmentBreakdown execute(String merchantId, long amountCents, int installments) {
        return calculator.calculate(merchantId, amountCents, installments);
    }
}