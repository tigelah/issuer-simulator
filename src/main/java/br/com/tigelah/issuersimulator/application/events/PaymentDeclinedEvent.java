package br.com.tigelah.issuersimulator.application.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentDeclinedEvent(
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String type,
        UUID paymentId,
        String reason
) { }