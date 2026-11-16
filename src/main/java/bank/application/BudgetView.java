package bank.application;

import bank.domain.BudgetStatus;

public record BudgetView(long categoryId, String categoryName, BudgetStatus status) {
}
