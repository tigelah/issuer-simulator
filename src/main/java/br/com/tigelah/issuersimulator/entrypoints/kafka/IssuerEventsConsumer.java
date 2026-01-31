package br.com.tigelah.issuersimulator.entrypoints.kafka;

import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;
import br.com.tigelah.issuersimulator.application.ports.EventPublisher;
import br.com.tigelah.issuersimulator.application.security.PanHasher;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeByIssuerUseCase;
import br.com.tigelah.issuersimulator.application.usecase.AuthorizeWithLimitsUseCase;
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
    private final AuthorizeByIssuerUseCase issuerUseCase;
    private final LedgerClient ledger;
    private final AuthorizeWithLimitsUseCase limitsUseCase;
    private final EventPublisher publisher;
    private final Clock clock;

    public IssuerEventsConsumer(
            ObjectMapper mapper,
            AuthorizeByIssuerUseCase issuerUseCase,
            LedgerClient ledger,
            AuthorizeWithLimitsUseCase limitsUseCase,
            EventPublisher publisher,
            Clock clock
    ) {
        this.mapper = mapper;
        this.issuerUseCase = issuerUseCase;
        this.ledger = ledger;
        this.limitsUseCase = limitsUseCase;
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
            if (correlationId != null && !correlationId.isBlank()) MDC.put("correlationId", correlationId);

            String type = root.path("type").asText("");
            UUID paymentId = parseUuid(root, "paymentId");
            if (paymentId == null) {
                log.warn("issuer_event_missing_paymentId type={} payload={}", type, root);
                return;
            }


            if ("payment.risk.rejected".equals(type)) {
                publishDeclined(paymentId, correlationId, "risk_rejected");
                log.info("issuer_declined_by_risk paymentId={}", paymentId);
                return;
            }


            if ("payment.risk.approved".equals(type)) {

                long amountCents = root.path("amountCents").asLong(0);
                if (amountCents <= 0) {
                    publishDeclined(paymentId, correlationId, "invalid_amount");
                    log.info("issuer_declined_invalid_amount paymentId={}", paymentId);
                    return;
                }


                UUID accountId = parseUuid(root, "accountId");
                if (accountId == null) {
                    publishDeclined(paymentId, correlationId, "account_required");
                    log.info("issuer_declined_missing_account paymentId={}", paymentId);
                    return;
                }

                String userId = root.path("userId").asText("");
                String panHash = resolvePanHash(root);


                var limitDecision = limitsUseCase.execute(accountId, amountCents, userId, panHash, ledger);
                if (!limitDecision.authorized()) {
                    publishDeclined(paymentId, correlationId, limitDecision.reason());
                    log.info("issuer_declined_by_limits paymentId={} reason={}", paymentId, limitDecision.reason());
                    return;
                }


                var issuerDecision = issuerUseCase.execute(amountCents, true);

                if (issuerDecision.approved()) {
                    publishAuthorized(paymentId, correlationId, issuerDecision.authCode());
                    log.info("issuer_authorized paymentId={}", paymentId);
                } else {
                    publishDeclined(paymentId, correlationId, issuerDecision.reason());
                    log.info("issuer_declined paymentId={} reason={}", paymentId, issuerDecision.reason());
                }
                return;
            }

            log.warn("issuer_unknown_event type={} payload={}", type, root);

        } catch (Exception e) {
            log.error("Failed to consume issuer message: {}", message, e);
        } finally {
            MDC.clear();
        }
    }

    private UUID parseUuid(JsonNode root, String field) {
        var raw = root.path(field).asText("");
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolvePanHash(JsonNode root) {
        var panHash = root.path("panHash").asText("");
        if (panHash != null && !panHash.isBlank()) return panHash;


        if (root.has("pan")) {
            var pan = root.path("pan").asText("");
            if (pan != null && !pan.isBlank()) return PanHasher.sha256(pan);
        }
        return "";
    }

    private void publishAuthorized(UUID paymentId, String correlationId, String authCode) {
        var authorized = new PaymentAuthorizedEvent(
                UUID.randomUUID(),
                Instant.now(clock),
                correlationId,
                Topics.PAYMENT_AUTHORIZED,
                paymentId,
                authCode != null && !authCode.isBlank() ? authCode : ("SIM" + System.nanoTime())
        );
        publisher.publishAuthorized(authorized);
    }

    private void publishDeclined(UUID paymentId, String correlationId, String reason) {
        var declined = new PaymentDeclinedEvent(
                UUID.randomUUID(),
                Instant.now(clock),
                correlationId,
                Topics.PAYMENT_DECLINED,
                paymentId,
                reason != null && !reason.isBlank() ? reason : "declined"
        );
        publisher.publishDeclined(declined);
    }
}