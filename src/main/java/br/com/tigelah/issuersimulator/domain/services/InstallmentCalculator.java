package br.com.tigelah.issuersimulator.domain.services;

import br.com.tigelah.issuersimulator.domain.model.InstallmentBreakdown;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

/**
 * Calcula o breakdown de uma compra parcelada (principal + juros + total + valor da parcela).
 *
 * <h2>Regras (passo a passo)</h2>
 *
 * <ol>
 *   <li><b>Normalização de entrada</b>:
 *     se {@code installments <= 0}, assumimos {@code installments = 1} (à vista).
 *   </li>
 *
 *   <li><b>Validação do plano</b>:
 *     o número de parcelas deve existir em {@code allowedPlans}.
 *     Caso contrário, lançamos {@code IllegalArgumentException("invalid_installments")}.
 *   </li>
 *
 *   <li><b>À vista</b>:
 *     se {@code installments == 1}, não há juros:
 *     <ul>
 *       <li>principal = amountCents</li>
 *       <li>interest = 0</li>
 *       <li>total = amountCents</li>
 *       <li>installmentAmount = amountCents</li>
 *     </ul>
 *   </li>
 *
 *   <li><b>Busca de taxa por merchant</b>:
 *     tentamos encontrar a taxa em {@code merchantRates.get(merchantId)}.
 *     Caso não exista, usamos {@code merchantRates.get("*")} como fallback global.
 *     Se ainda assim não existir taxa para aquele número de parcelas,
 *     lançamos {@code IllegalArgumentException("installments_not_supported")}.
 *   </li>
 *
 *   <li><b>Cálculo de juros</b>:
 *     {@code interest = amountCents * rate}, arredondando para o centavo mais próximo
 *     usando {@link RoundingMode#HALF_UP}.
 *   </li>
 *
 *   <li><b>Total</b>:
 *     {@code total = principal + interest}.
 *   </li>
 *
 *   <li><b>Valor da parcela</b>:
 *     usamos arredondamento para cima (ceil) ao dividir o total por parcelas:
 *     {@code installmentAmount = ceil(total / installments)}.
 *     <p>
 *     Isso evita "perder" centavos no parcelamento. Em sistemas reais, o ajuste de centavos
 *     pode ser distribuído entre parcelas (ex.: última parcela), mas aqui usamos uma regra
 *     simples e determinística.
 *   </li>
 * </ol>
 *
 * <p><b>Observação:</b> este cálculo é determinístico e adequado para simulação e testes.
 * Em produção, taxas podem vir de tabela/config store, e o split de centavos pode seguir
 * regra contábil específica (ex.: last installment adjustment).
 */
public class InstallmentCalculator {

    private final Set<Integer> allowedPlans;
    private final Map<String, Map<Integer, BigDecimal>> merchantRates; // merchantId -> (installments -> rate)

    public InstallmentCalculator(Set<Integer> allowedPlans,
                                 Map<String, Map<Integer, BigDecimal>> merchantRates) {
        this.allowedPlans = allowedPlans;
        this.merchantRates = merchantRates;
    }

    public InstallmentBreakdown calculate(String merchantId, long amountCents, int installments) {
        if (installments <= 0) installments = 1;

        if (!allowedPlans.contains(installments)) {
            throw new IllegalArgumentException("invalid_installments");
        }

        if (installments == 1) {
            return new InstallmentBreakdown(1, amountCents, 0, amountCents, amountCents);
        }

        var rate = rateFor(merchantId, installments);
        var principal = BigDecimal.valueOf(amountCents);
        var interest = principal.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValue();

        long total = amountCents + interest;

        long installmentAmount = (total + installments - 1) / installments;

        return new InstallmentBreakdown(installments, amountCents, interest, total, installmentAmount);
    }

    private BigDecimal rateFor(String merchantId, int installments) {
        var byMerchant = merchantRates.getOrDefault(merchantId, merchantRates.get("*"));
        if (byMerchant == null) throw new IllegalArgumentException("installments_not_supported");

        var rate = byMerchant.get(installments);
        if (rate == null) throw new IllegalArgumentException("installments_not_supported");

        return rate;
    }
}