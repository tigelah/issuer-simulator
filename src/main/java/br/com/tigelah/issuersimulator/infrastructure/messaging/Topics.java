package br.com.tigelah.issuersimulator.infrastructure.messaging;

public final class Topics {
    private Topics() {}
    public static final String PAYMENT_RISK_APPROVED = "payment.risk.approved";
    public static final String PAYMENT_RISK_REJECTED = "payment.risk.rejected";

    public static final String PAYMENT_AUTHORIZED = "payment.authorized";
    public static final String PAYMENT_DECLINED = "payment.declined";
}

