package br.com.tigelah.issuersimulator.application.usecase;

import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizeWithLimitsUseCaseTest {

    @Test
    void declines_when_insufficient_funds() {
        var uc = new AuthorizeWithLimitsUseCase();
        LedgerClient ledger = new FakeLedger(50, Optional.empty(), Optional.empty());

        var d = uc.execute(UUID.randomUUID(), 60, "u1", "h1", ledger);
        assertFalse(d.authorized());
        assertEquals("insufficient_funds", d.reason());
    }

    @Test
    void declines_when_daily_limit_exceeded() {
        var uc = new AuthorizeWithLimitsUseCase();
        var rule = new LedgerClient.LimitRule("USER","u1","BRL", 0, 10, 0);
        LedgerClient ledger = new FakeLedger(100, Optional.of(rule), Optional.empty());

        var d = uc.execute(UUID.randomUUID(), 11, "u1", "", ledger);
        assertFalse(d.authorized());
        assertEquals("limit_exceeded", d.reason());
    }

    @Test
    void approves_when_within_limits_and_credit() {
        var uc = new AuthorizeWithLimitsUseCase();
        LedgerClient ledger = new FakeLedger(100, Optional.empty(), Optional.empty());

        var d = uc.execute(UUID.randomUUID(), 10, "u1", "h1", ledger);
        assertTrue(d.authorized());
    }

    static class FakeLedger extends LedgerClient {
        private final long available;
        private final Optional<LimitRule> userRule;
        private final Optional<LimitRule> panRule;

        FakeLedger(long available, Optional<LimitRule> userRule, Optional<LimitRule> panRule) {
            super(null, "http://x", new ObjectMapper());
            this.available = available;
            this.userRule = userRule;
            this.panRule = panRule;
        }

        @Override public AvailableCredit getAvailableCredit(UUID accountId, String correlationId) {
            return new AvailableCredit(accountId, available, "BRL", 0, 0);
        }

        @Override public Optional<LimitRule> getUserLimit(String userId) { return userRule; }
        @Override public Optional<LimitRule> getPanLimit(String panHash) { return panRule; }
    }
}
