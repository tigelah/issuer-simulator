package br.com.tigelah.issuersimulator.entrypoints.kafka;

import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;
import br.com.tigelah.issuersimulator.application.ports.EventPublisher;
import br.com.tigelah.issuersimulator.application.security.PanHasher;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeByIssuerUseCase;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeWithLimitsUseCase;
import br.com.tigelah.issuersimulator.application.usecase.CalculateInstallmentsUseCase;
import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;
import br.com.tigelah.issuersimulator.infrastructure.messaging.Topics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class IssuerEventsConsumer {
    private static final Logger log = LoggerFactory.getLogger(IssuerEventsConsumer.class);

    private final ObjectMapper mapper;
    private final AuthorizeByIssuerUseCase useCase;
    private final LedgerClient ledger;
    private final AuthorizeWithLimitsUseCase limitsUseCase;
    private final CalculateInstallmentsUseCase installmentsUseCase; // NOVO
    private final EventPublisher publisher;
    private final Clock clock;

    public IssuerEventsConsumer(
            ObjectMapper mapper,
            AuthorizeByIssuerUseCase useCase,
            LedgerClient ledger,
            AuthorizeWithLimitsUseCase limitsUseCase,
            CalculateInstallmentsUseCase installmentsUseCase, // NOVO
            EventPublisher publisher,
            Clock clock
    ) {
        this.mapper = mapper;
        this.useCase = useCase;
        this.ledger = ledger;
        this.limitsUseCase = limitsUseCase;
        this.installmentsUseCase = installmentsUseCase;
        this.publisher = publisher;
        this.clock = clock;
    }

    @KafkaListener(
            topics = { Topics.PAYMENT_RISK_APPROVED, Topics.PAYMENT_RISK_REJECTED },
            groupId = "${kafka.consumer.group-id:issuer-simulator}"
    )
    public void onMessage(String message) {
        try {
            JsonNode root = mapper.readTree(message);

            String correlationId = root.path("correlationId").asText(null);
            if (correlationId != null) MDC.put("correlationId", correlationId);

            String type = root.path("type").asText("");
            UUID paymentId = UUID.fromString(root.path("paymentId").asText());

            if ("payment.risk.rejected".equals(type)) {
                publishDeclined(paymentId, correlationId, "risk_rejected");
                return;
            }

            if ("payment.risk.approved".equals(type)) {

                long amountCents = root.path("amountCents").asLong(0);
                boolean riskApproved = root.path("approved").asBoolean(true);


                var accountId = UUID.fromString(root.path("accountId").asText());
                var merchantId = root.path("merchantId").asText("");
                var userId = root.path("userId").asText("");
                var panHash = root.path("panHash").asText("");

                if ((panHash == null || panHash.isBlank()) && root.has("pan")) {
                    var pan = root.path("pan").asText("");
                    if (!pan.isBlank()) panHash = PanHasher.sha256(pan);
                }

                int installments = root.path("installments").asInt(1);

                var issuerDecision = useCase.execute(amountCents, riskApproved);
                if (!issuerDecision.approved()) {
                    publishDeclined(paymentId, correlationId, issuerDecision.reason());
                    return;
                }

                var limitDecision = limitsUseCase.execute(accountId, amountCents, userId, panHash, ledger);
                if (!limitDecision.authorized()) {
                    publishDeclined(paymentId, correlationId, limitDecision.reason());
                    return;
                }

                try {
                    var breakdown = installmentsUseCase.execute(merchantId, amountCents, installments);

                    var authorized = new PaymentAuthorizedEvent(
                            UUID.randomUUID(),
                            Instant.now(clock),
                            correlationId,
                            Topics.PAYMENT_AUTHORIZED,
                            paymentId,
                            limitDecision.authCode(),
                            breakdown.installments(),
                            breakdown.interestCents(),
                            breakdown.totalCents(),
                            breakdown.installmentAmountCents()
                    );

                    publisher.publishAuthorized(authorized);
                    log.info("issuer_authorized paymentId={} installments={} totalCents={} interestCents={}",
                            paymentId, breakdown.installments(), breakdown.totalCents(), breakdown.interestCents());
                    return;

                } catch (IllegalArgumentException ex) {
                    var reason = ex.getMessage();
                    if (!"invalid_installments".equals(reason) && !"installments_not_supported".equals(reason)) {
                        reason = "invalid_installments";
                    }
                    publishDeclined(paymentId, correlationId, reason);
                    log.info("issuer_declined_installments paymentId={} installments={} reason={}", paymentId, installments, reason);
                    return;
                }
            }

            log.warn("issuer_unknown_event type={} payload={}", type, root);

        } catch (Exception e) {
            log.error("Failed to consume issuer message: {}", message, e);
        } finally {
            MDC.clear();
        }
    }

    private void publishDeclined(UUID paymentId, String correlationId, String reason) {
        var declined = new PaymentDeclinedEvent(
                UUID.randomUUID(),
                Instant.now(clock),
                correlationId,
                Topics.PAYMENT_DECLINED,
                paymentId,
                reason
        );
        publisher.publishDeclined(declined);
    }
}