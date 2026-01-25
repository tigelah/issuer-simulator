package br.com.tigelah.issuersimulator.domain.model;

import br.com.tigelah.issuersimulator.domain.services.SimpleIssuerRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleIssuerRulesTest {

    @Test
    void decline_when_risk_rejected() {
        var rules = new SimpleIssuerRules(1000);

        var d = rules.authorize(10, false);

        assertFalse(d.approved());
        assertEquals("risk_rejected", d.reason());
        assertNull(d.authCode());
    }

    @Test
    void authorize_under_limit() {
        var rules = new SimpleIssuerRules(1000);

        var d = rules.authorize(10, true);

        assertTrue(d.approved());
        assertNotNull(d.authCode());
    }
}
