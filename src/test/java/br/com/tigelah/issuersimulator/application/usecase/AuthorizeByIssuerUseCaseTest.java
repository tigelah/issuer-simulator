package br.com.tigelah.issuersimulator.application.usecase;

import br.com.tigelah.issuersimulator.domain.services.SimpleIssuerRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizeByIssuerUseCaseTest {

    @Test
    void delegates() {
        var uc = new AuthorizeByIssuerUseCase(new SimpleIssuerRules(1000));

        var decision = uc.execute(1, true);

        assertTrue(decision.approved());
        assertNotNull(decision.authCode());
    }
}
