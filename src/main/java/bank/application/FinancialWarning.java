package bank.application;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record FinancialWarning(
        WarningType type,
        String category,
        BigDecimal actual,
        BigDecimal limit) {

    public FinancialWarning {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(limit, "limit");
    }

    public Optional<String> categoryName() {
        return Optional.ofNullable(category);
    }
}
