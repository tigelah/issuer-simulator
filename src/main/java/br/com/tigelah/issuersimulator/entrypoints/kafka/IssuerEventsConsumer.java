package br.com.tigelah.issuersimulator.entrypoints.kafka;

import br.com.tigelah.issuersimulator.application.usecase.AuthorizeByIssuerUseCase;
import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;
import br.com.tigelah.issuersimulator.application.ports.EventPublisher;
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
    private final EventPublisher publisher;
    private final Clock clock;

    public IssuerEventsConsumer(ObjectMapper mapper, AuthorizeByIssuerUseCase useCase, EventPublisher publisher, Clock clock) {
        this.mapper = mapper;
        this.useCase = useCase;
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

            if (Topics.PAYMENT_RISK_REJECTED.equals(type) || "payment.risk.rejected".equals(type)) {
                var declined = new PaymentDeclinedEvent(
                        UUID.randomUUID(),
                        Instant.now(clock),
                        correlationId,
                        Topics.PAYMENT_DECLINED,
                        paymentId,
                        "risk_rejected"
                );
                publisher.publishDeclined(declined);
                log.info("issuer_declined_by_risk paymentId={}", paymentId);
                return;
            }

            if (Topics.PAYMENT_RISK_APPROVED.equals(type) || "payment.risk.approved".equals(type)) {
                long amountCents = root.path("amountCents").asLong(0);

                boolean riskApproved = root.path("approved").asBoolean(false);

                var decision = useCase.execute(amountCents, riskApproved);

                if (decision.approved()) {
                    var authorized = new PaymentAuthorizedEvent(
                            UUID.randomUUID(),
                            Instant.now(clock),
                            correlationId,
                            Topics.PAYMENT_AUTHORIZED,
                            paymentId,
                            decision.authCode()
                    );
                    publisher.publishAuthorized(authorized);
                    log.info("issuer_authorized paymentId={}", paymentId);
                } else {
                    var declined = new PaymentDeclinedEvent(
                            UUID.randomUUID(),
                            Instant.now(clock),
                            correlationId,
                            Topics.PAYMENT_DECLINED,
                            paymentId,
                            decision.reason()
                    );
                    publisher.publishDeclined(declined);
                    log.info("issuer_declined paymentId={} reason={}", paymentId, decision.reason());
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
}