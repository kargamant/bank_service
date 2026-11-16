package bank.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class BudgetStatus {

    private final BigDecimal limit;
    private final BigDecimal spent;
    private final BigDecimal remaining;
    private final boolean exceeded;

    public BudgetStatus(BigDecimal limit, BigDecimal spent) {
        this.limit = DomainValidation.requirePositiveMoney(limit, "budget limit");
        this.spent = DomainValidation.requireNonNegativeMoney(spent, "budget expenses");
        this.remaining = this.limit.subtract(this.spent);
        this.exceeded = remaining.signum() < 0;
    }

    public BigDecimal limit() {
        return limit;
    }

    public BigDecimal spent() {
        return spent;
    }

    public BigDecimal remaining() {
        return remaining;
    }

    public boolean exceeded() {
        return exceeded;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BudgetStatus status)) {
            return false;
        }
        return exceeded == status.exceeded
                && limit.compareTo(status.limit) == 0
                && spent.compareTo(status.spent) == 0
                && remaining.compareTo(status.remaining) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                limit.stripTrailingZeros(),
                spent.stripTrailingZeros(),
                remaining.stripTrailingZeros(),
                exceeded);
    }
}
