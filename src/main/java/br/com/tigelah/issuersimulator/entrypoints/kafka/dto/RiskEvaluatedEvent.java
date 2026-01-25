package br.com.tigelah.issuersimulator.entrypoints.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RiskEvaluatedEvent(
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String type,
        UUID paymentId,
        boolean approved,
        String reason
) { }
