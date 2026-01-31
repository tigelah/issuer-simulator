package br.com.tigelah.issuersimulator.infrastructure.config;

import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LedgerConfig {

    @Bean
    RestTemplate restTemplate() { return new RestTemplate(); }

    @Bean
    LedgerClient ledgerClient(RestTemplate restTemplate,
                              @Value("${issuer.ledger.base-url}") String baseUrl,
                              ObjectMapper mapper) {
        return new LedgerClient(restTemplate, baseUrl, mapper);
    }
}
