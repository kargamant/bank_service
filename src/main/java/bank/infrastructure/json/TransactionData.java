package bank.infrastructure.json;

import bank.domain.Transaction;
import bank.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

record TransactionData(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        LocalDateTime createdAt,
        String description,
        UUID transferId) {

    static TransactionData fromDomain(Transaction transaction) {
        return new TransactionData(
                transaction.id(),
                transaction.type(),
                transaction.amount(),
                transaction.createdAt(),
                transaction.description().orElse(null),
                transaction.transferId().orElse(null));
    }

    Transaction toDomain() {
        return new Transaction(id, type, amount, createdAt, description, transferId);
    }
}
