package br.com.tigelah.issuersimulator.infrastructure.config;

import br.com.tigelah.issuersimulator.application.usecase.AuthorizeByIssuerUseCase;
import br.com.tigelah.issuersimulator.domain.services.SimpleIssuerRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    @Bean
    public SimpleIssuerRules simpleIssuerRules(@Value("${issuer.max-amount-cents:1000000}") long maxAmountCents) {
        return new SimpleIssuerRules(maxAmountCents);
    }

    @Bean
    public AuthorizeByIssuerUseCase authorizeByIssuerUseCase(SimpleIssuerRules rules) {
        return new AuthorizeByIssuerUseCase(rules);
    }
}