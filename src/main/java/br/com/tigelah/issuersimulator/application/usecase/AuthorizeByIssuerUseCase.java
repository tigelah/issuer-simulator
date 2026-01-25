package br.com.tigelah.issuersimulator.application.usecase;

import br.com.tigelah.issuersimulator.domain.model.IssuerDecision;
import br.com.tigelah.issuersimulator.domain.services.SimpleIssuerRules;

public class AuthorizeByIssuerUseCase {
    private final SimpleIssuerRules rules;
    public AuthorizeByIssuerUseCase(SimpleIssuerRules rules) { this.rules = rules; }
    public IssuerDecision execute(long amountCents, boolean riskApproved) { return rules.authorize(amountCents, riskApproved); }
}
