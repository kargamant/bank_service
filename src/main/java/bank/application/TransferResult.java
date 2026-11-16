package bank.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record TransferResult(
        UUID transferId,
        String senderLogin,
        String recipientLogin,
        BigDecimal amount,
        BigDecimal senderBalance,
        BigDecimal recipientBalance,
        List<FinancialWarning> warnings) {

    public TransferResult {
        Objects.requireNonNull(transferId, "transferId");
        Objects.requireNonNull(senderLogin, "senderLogin");
        Objects.requireNonNull(recipientLogin, "recipientLogin");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(senderBalance, "senderBalance");
        Objects.requireNonNull(recipientBalance, "recipientBalance");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
}
