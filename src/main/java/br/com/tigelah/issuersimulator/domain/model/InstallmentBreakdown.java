package br.com.tigelah.issuersimulator.domain.model;

public record InstallmentBreakdown(
        Integer installments,
        long principalCents,
        long interestCents,
        long totalCents,
        long installmentAmountCents
) { }
