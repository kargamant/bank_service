package bank.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Transaction {

    private final UUID id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;
    private final String description;
    private final UUID transferId;

    public Transaction(
            UUID id,
            TransactionType type,
            BigDecimal amount,
            LocalDateTime createdAt,
            String description,
            UUID transferId) {
        this.id = DomainValidation.requireNotNull(id, "id not specified");
        this.type = DomainValidation.requireNotNull(type, "no transaction type");
        this.amount = DomainValidation.requirePositiveMoney(amount, "transaction amount");
        this.createdAt = DomainValidation.requireNotNull(
                createdAt, "no date and time specified for transaction");
        this.description = normalizeDescription(description);
        this.transferId = transferId;
    }

    public static Transaction create(
            TransactionType type, BigDecimal amount, String description) {
        return new Transaction(
                UUID.randomUUID(), type, amount, LocalDateTime.now(), description, null);
    }

    public static Transaction createTransfer(
            TransactionType type,
            BigDecimal amount,
            String description,
            UUID transferId) {
        return new Transaction(
                UUID.randomUUID(),
                type,
                amount,
                LocalDateTime.now(),
                description,
                DomainValidation.requireNotNull(
                        transferId, "transfer id not specified"));
    }

    public UUID id() {
        return id;
    }

    public TransactionType type() {
        return type;
    }

    public BigDecimal amount() {
        return amount;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<UUID> transferId() {
        return Optional.ofNullable(transferId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Transaction transaction)) {
            return false;
        }
        return id.equals(transaction.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty()
                ? null
                : DomainValidation.requireLettersAndSpaces(
                        trimmed, "transaction description");
    }
}
