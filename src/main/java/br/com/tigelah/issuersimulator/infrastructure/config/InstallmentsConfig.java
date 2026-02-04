package br.com.tigelah.issuersimulator.infrastructure.config;

import br.com.tigelah.issuersimulator.application.usecase.CalculateInstallmentsUseCase;
import br.com.tigelah.issuersimulator.domain.services.InstallmentCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Configuration
public class InstallmentsConfig {

    @Bean
    public CalculateInstallmentsUseCase calculateInstallmentsUseCase() {

        var allowed = Set.of(1, 2, 6, 12);

        var rates = Map.<String, Map<Integer, BigDecimal>>of(
                "*", Map.of(
                        2, new BigDecimal("0.020"),   // 2%
                        6, new BigDecimal("0.060"),   // 6%
                        12, new BigDecimal("0.120")   // 12%
                ),
                "m1", Map.of(
                        2, new BigDecimal("0.015"),
                        6, new BigDecimal("0.050"),
                        12, new BigDecimal("0.100")
                )
        );

        return new CalculateInstallmentsUseCase(new InstallmentCalculator(allowed, rates));
    }
}
