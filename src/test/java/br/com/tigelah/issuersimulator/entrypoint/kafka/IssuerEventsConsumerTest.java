package br.com.tigelah.issuersimulator.entrypoints.kafka;

import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;
import br.com.tigelah.issuersimulator.application.ports.EventPublisher;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeByIssuerUseCase;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeWithLimitsUseCase;
import br.com.tigelah.issuersimulator.application.usecase.CalculateInstallmentsUseCase;
import br.com.tigelah.issuersimulator.domain.services.SimpleIssuerRules;
import br.com.tigelah.issuersimulator.domain.services.InstallmentCalculator;
import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IssuerEventsConsumerTest {

    @Test
    void authorizes_with_breakdown_when_installments_valid() {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

        var issuerUc = new AuthorizeByIssuerUseCase(new SimpleIssuerRules(999999));
        var ledger = mock(LedgerClient.class);

        // limites sempre aprovados neste teste
        var limitsUc = mock(AuthorizeWithLimitsUseCase.class);
        when(limitsUc.execute(any(), anyLong(), anyString(), anyString(), any()))
                .thenReturn(new AuthorizeWithLimitsUseCase.LimitDecision(true, "ok", "SIM123"));

        // juros 10% para 12x (merchant m1)
        var calc = new InstallmentCalculator(Set.of(1,2,6,12), Map.of(
                "m1", Map.of(12, new BigDecimal("0.10")),
                "*", Map.of(12, new BigDecimal("0.12"))
        ));
        var installmentsUc = new CalculateInstallmentsUseCase(calc);

        var authRef = new AtomicReference<PaymentAuthorizedEvent>();
        var decRef = new AtomicReference<PaymentDeclinedEvent>();
        EventPublisher publisher = new EventPublisher() {
            @Override public void publishAuthorized(PaymentAuthorizedEvent event) { authRef.set(event); }
            @Override public void publishDeclined(PaymentDeclinedEvent event) { decRef.set(event); }
        };

        var consumer = new IssuerEventsConsumer(mapper, issuerUc, ledger, limitsUc, installmentsUc, publisher, clock);

        var paymentId = UUID.randomUUID();
        var accountId = UUID.randomUUID();

        var msg = """
        {
          "eventId":"%s",
          "occurredAt":"2030-01-01T00:00:00Z",
          "correlationId":"c1",
          "type":"payment.risk.approved",
          "paymentId":"%s",
          "approved":true,
          "reason":"ok",
          "merchantId":"m1",
          "amountCents":1000,
          "currency":"BRL",
          "installments":12,
          "accountId":"%s",
          "userId":"u1",
          "panHash":"h1",
          "panLast4":"1111"
        }
        """.formatted(UUID.randomUUID(), paymentId, accountId);

        consumer.onMessage(msg);

        assertNull(decRef.get());
        assertNotNull(authRef.get());
        assertEquals(12, authRef.get().installments());
        assertEquals(1000L, authRef.get().totalCents() - authRef.get().interestCents());
        assertTrue(authRef.get().interestCents() > 0);
        assertTrue(authRef.get().installmentAmountCents() > 0);
    }

    @Test
    void declines_when_installments_invalid() {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

        var issuerUc = new AuthorizeByIssuerUseCase(new SimpleIssuerRules(999999));
        var ledger = mock(LedgerClient.class);

        var limitsUc = mock(AuthorizeWithLimitsUseCase.class);
        when(limitsUc.execute(any(), anyLong(), anyString(), anyString(), any()))
                .thenReturn(new AuthorizeWithLimitsUseCase.LimitDecision(true, "ok", "SIM123"));

        var calc = new InstallmentCalculator(Set.of(1,2,6,12), Map.of("*", Map.of(2, new BigDecimal("0.02"))));
        var installmentsUc = new CalculateInstallmentsUseCase(calc);

        var authRef = new AtomicReference<PaymentAuthorizedEvent>();
        var decRef = new AtomicReference<PaymentDeclinedEvent>();
        EventPublisher publisher = new EventPublisher() {
            @Override public void publishAuthorized(PaymentAuthorizedEvent event) { authRef.set(event); }
            @Override public void publishDeclined(PaymentDeclinedEvent event) { decRef.set(event); }
        };

        var consumer = new IssuerEventsConsumer(mapper, issuerUc, ledger, limitsUc, installmentsUc, publisher, clock);

        var msg = """
        {
          "eventId":"%s",
          "occurredAt":"2030-01-01T00:00:00Z",
          "correlationId":"c1",
          "type":"payment.risk.approved",
          "paymentId":"%s",
          "approved":true,
          "merchantId":"m1",
          "amountCents":1000,
          "currency":"BRL",
          "installments":5,
          "accountId":"%s",
          "panHash":"h1",
          "panLast4":"1111"
        }
        """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        consumer.onMessage(msg);

        assertNull(authRef.get());
        assertNotNull(decRef.get());
        assertEquals("invalid_installments", decRef.get().reason());
    }
}