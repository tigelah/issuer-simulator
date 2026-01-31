package br.com.tigelah.issuersimulator.application.usecase;

import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;

import java.util.Optional;
import java.util.UUID;

public class AuthorizeWithLimitsUseCase {

    public Decision execute(UUID accountId, long amountCents, String userId, String panHash, LedgerClient ledger) {
        var available = ledger.getAvailableCredit(accountId, "n/a");

        if (amountCents > available.availableCents()) {
            return Decision.decline("insufficient_funds");
        }

        Optional<LedgerClient.LimitRule> rule = Optional.empty();
        if (userId != null && !userId.isBlank()) rule = ledger.getUserLimit(userId);
        if (rule.isEmpty() && panHash != null && !panHash.isBlank()) rule = ledger.getPanLimit(panHash);

        if (rule.isPresent()) {
            var r = rule.get();
            if (r.creditLimitCents() > 0 && amountCents > r.creditLimitCents()) return Decision.decline("limit_exceeded");
            if (r.dailyLimitCents() > 0 && amountCents > r.dailyLimitCents()) return Decision.decline("limit_exceeded");
            if (r.monthlyLimitCents() > 0 && amountCents > r.monthlyLimitCents()) return Decision.decline("limit_exceeded");
        }

        return Decision.approve();
    }

    public record Decision(boolean authorized, String reason) {
        public static Decision approve() { return new Decision(true, "ok"); }
        public static Decision decline(String reason) { return new Decision(false, reason); }
    }
}
