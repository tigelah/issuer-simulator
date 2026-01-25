package br.com.tigelah.issuersimulator.infrastructure.messaging;

import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;
import br.com.tigelah.issuersimulator.application.ports.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, Object> kafka;
    public KafkaEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    @Override
    public void publishAuthorized(PaymentAuthorizedEvent event) {
        kafka.send(Topics.PAYMENT_AUTHORIZED, event.paymentId().toString(), event);
    }

    @Override
    public void publishDeclined(PaymentDeclinedEvent event) {
        kafka.send(Topics.PAYMENT_DECLINED, event.paymentId().toString(), event);
    }
}
