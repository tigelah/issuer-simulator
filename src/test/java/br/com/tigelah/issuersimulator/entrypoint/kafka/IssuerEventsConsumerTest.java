package br.com.tigelah.issuersimulator.entrypoint.kafka;

import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;
import br.com.tigelah.issuersimulator.application.ports.EventPublisher;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeByIssuerUseCase;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeWithLimitsUseCase;
import br.com.tigelah.issuersimulator.domain.services.SimpleIssuerRules;
import br.com.tigelah.issuersimulator.entrypoints.kafka.IssuerEventsConsumer;
import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IssuerEventsConsumerTest {

    @Test
    void publishes_declined_when_risk_rejected() {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var issuerUc = new AuthorizeByIssuerUseCase(new SimpleIssuerRules(1000));
        var clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

        LedgerClient ledger = mock(LedgerClient.class);
        AuthorizeWithLimitsUseCase limitsUc = mock(AuthorizeWithLimitsUseCase.class);

        var authRef = new AtomicReference<PaymentAuthorizedEvent>();
        var decRef = new AtomicReference<PaymentDeclinedEvent>();

        EventPublisher publisher = new EventPublisher() {
            @Override public void publishAuthorized(PaymentAuthorizedEvent event) { authRef.set(event); }
            @Override public void publishDeclined(PaymentDeclinedEvent event) { decRef.set(event); }
        };

        var consumer = new IssuerEventsConsumer(mapper, issuerUc, ledger, limitsUc, publisher, clock);

        var msg = String.format(
                "{\"eventId\":\"%s\",\"occurredAt\":\"2030-01-01T00:00:00Z\",\"correlationId\":\"c1\",\"type\":\"payment.risk.rejected\",\"paymentId\":\"%s\",\"approved\":false,\"reason\":\"pan_blocked\"}",
                UUID.randomUUID(), UUID.randomUUID()
        );

        consumer.onMessage(msg);

        assertNull(authRef.get());
        assertNotNull(decRef.get());
        assertEquals("risk_rejected", decRef.get().reason());


        verifyNoInteractions(ledger);
        verifyNoInteractions(limitsUc);
    }
}