package br.com.tigelah.issuersimulator.domain.services;

import br.com.tigelah.issuersimulator.domain.model.IssuerDecision;

public record SimpleIssuerRules(long maxAmountCents) {

    public IssuerDecision authorize(long amountCents, boolean riskApproved) {
        if (!riskApproved) {
            return IssuerDecision.rejected("risk_rejected");
        }
        if (amountCents <= 0) {
            return IssuerDecision.rejected("amount_invalid");
        }
        if (amountCents > maxAmountCents) {
            return IssuerDecision.rejected("amount_exceeded");
        }
        return IssuerDecision.approved("ISSUER_OK");
    }
}