package br.com.tigelah.issuersimulator.domain.model;

public record IssuerDecision(boolean approved, String reason, String authCode) {

    public static IssuerDecision approved(String authCode) {
        return new IssuerDecision(true, null, authCode);
    }

    public static IssuerDecision rejected(String reason) {
        return new IssuerDecision(false, reason, null);
    }
}
