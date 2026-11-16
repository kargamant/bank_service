package bank.application;

import bank.domain.TransactionType;

public record CategoryView(
        long id,
        String name,
        TransactionType type,
        int transactionCount,
        boolean hasBudget) {
}
