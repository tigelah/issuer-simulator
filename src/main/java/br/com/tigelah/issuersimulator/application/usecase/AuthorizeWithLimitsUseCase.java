package br.com.tigelah.issuersimulator.application.usecase;

import br.com.tigelah.issuersimulator.infrastructure.http.LedgerClient;

import java.util.Optional;
import java.util.UUID;

public class AuthorizeWithLimitsUseCase {

    public LimitDecision execute(UUID accountId, long amountCents, String userId, String panHash, LedgerClient ledger) {
        var available = ledger.getAvailableCredit(accountId, "n/a");

        if (amountCents > available.availableCents()) {
            return LimitDecision.decline("insufficient_funds");
        }

        Optional<LedgerClient.LimitRule> rule = Optional.empty();
        if (userId != null && !userId.isBlank()) rule = ledger.getUserLimit(userId);
        if (rule.isEmpty() && panHash != null && !panHash.isBlank()) rule = ledger.getPanLimit(panHash);

        if (rule.isPresent()) {
            var r = rule.get();
            if (r.creditLimitCents() > 0 && amountCents > r.creditLimitCents()) return LimitDecision.decline("limit_exceeded");
            if (r.dailyLimitCents() > 0 && amountCents > r.dailyLimitCents()) return LimitDecision.decline("limit_exceeded");
            if (r.monthlyLimitCents() > 0 && amountCents > r.monthlyLimitCents()) return LimitDecision.decline("limit_exceeded");
        }
        return LimitDecision.approve(generateAuthCode());
    }

    private String generateAuthCode() {
        return "SIM" + Math.abs(UUID.randomUUID().hashCode());
    }

    public record LimitDecision(boolean authorized, String reason, String authCode) {
        public static LimitDecision approve(String authCode) {
            if (authCode == null || authCode.isBlank()) throw new IllegalArgumentException("authCode_required");
            return new LimitDecision(true, "ok", authCode);
        }

        public static LimitDecision decline(String reason) {
            if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason_required");
            return new LimitDecision(false, reason, null);
        }
    }
}