package bank.application;

import bank.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public record TransactionView(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        String category,
        LocalDateTime createdAt,
        String description,
        UUID transferId) {

    public Optional<String> optionalDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<UUID> optionalTransferId() {
        return Optional.ofNullable(transferId);
    }
}
