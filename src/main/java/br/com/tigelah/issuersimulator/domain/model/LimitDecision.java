package br.com.tigelah.issuersimulator.domain.model;

public record LimitDecision(
        boolean authorized,
        String reason,
        String authCode
) {

    public static LimitDecision approved(String authCode) {
        return new LimitDecision(true, null, authCode);
    }

    public static LimitDecision declined(String reason) {
        return new LimitDecision(false, reason, null);
    }
}
