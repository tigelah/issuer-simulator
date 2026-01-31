package br.com.tigelah.issuersimulator.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

public class LedgerClient {

    private final RestTemplate http;
    private final String baseUrl;
    private final ObjectMapper mapper;

    public LedgerClient(RestTemplate http, String baseUrl, ObjectMapper mapper) {
        this.http = http;
        this.baseUrl = baseUrl;
        this.mapper = mapper;
    }

    public AvailableCredit getAvailableCredit(UUID accountId, String correlationId) {
        var url = baseUrl + "/accounts/" + accountId + "/available-credit";
        var res = http.getForEntity(url, String.class);
        return parseAvailable(res);
    }

    public Optional<LimitRule> getUserLimit(String userId) { return getLimit("/limits/users/" + userId); }
    public Optional<LimitRule> getPanLimit(String panHash) { return getLimit("/limits/pan/" + panHash); }

    private Optional<LimitRule> getLimit(String path) {
        try {
            var res = http.getForEntity(baseUrl + path, String.class);
            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return Optional.empty();
            var root = mapper.readTree(res.getBody());
            return Optional.of(new LimitRule(
                    root.path("scopeType").asText(),
                    root.path("scopeKey").asText(),
                    root.path("currency").asText("BRL"),
                    root.path("creditLimitCents").asLong(0),
                    root.path("dailyLimitCents").asLong(0),
                    root.path("monthlyLimitCents").asLong(0)
            ));
        } catch (HttpClientErrorException.NotFound nf) {
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("failed_to_call_ledger", e);
        }
    }

    private AvailableCredit parseAvailable(ResponseEntity<String> res) {
        try {
            var root = mapper.readTree(res.getBody());
            return new AvailableCredit(
                    UUID.fromString(root.path("accountId").asText()),
                    root.path("availableCents").asLong(),
                    root.path("currency").asText("BRL"),
                    root.path("holdsCents").asLong(0),
                    root.path("capturedCents").asLong(0)
            );
        } catch (Exception e) {
            throw new IllegalStateException("failed_to_parse_available_credit", e);
        }
    }

    public record AvailableCredit(UUID accountId, long availableCents, String currency, long holdsCents, long capturedCents) {}
    public record LimitRule(String scopeType, String scopeKey, String currency, long creditLimitCents, long dailyLimitCents, long monthlyLimitCents) {}
}
