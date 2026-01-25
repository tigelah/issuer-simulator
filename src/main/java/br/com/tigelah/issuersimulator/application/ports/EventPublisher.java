package br.com.tigelah.issuersimulator.application.ports;

import br.com.tigelah.issuersimulator.application.events.PaymentAuthorizedEvent;
import br.com.tigelah.issuersimulator.application.events.PaymentDeclinedEvent;

public interface EventPublisher {
    void publishAuthorized(PaymentAuthorizedEvent event);
    void publishDeclined(PaymentDeclinedEvent event);
}
