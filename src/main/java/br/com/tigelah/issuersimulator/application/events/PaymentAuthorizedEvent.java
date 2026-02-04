package br.com.tigelah.issuersimulator.application.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentAuthorizedEvent(
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String type,
        UUID paymentId,
        String authCode,
        Integer installments,
        long interestCents,
        long totalCents,
        long installmentAmountCents
) { }