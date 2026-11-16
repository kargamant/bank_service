package bank.application;

import bank.domain.BudgetStatus;
import bank.domain.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TransactionResult(
        Transaction transaction,
        BigDecimal balance,
        BudgetStatus budgetStatus,
        List<FinancialWarning> warnings) {

    public TransactionResult {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(balance, "balance");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    public Optional<BudgetStatus> optionalBudgetStatus() {
        return Optional.ofNullable(budgetStatus);
    }
}
