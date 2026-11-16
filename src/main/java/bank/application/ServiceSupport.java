package bank.application;

import bank.application.exception.ResourceNotFoundException;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.WalletRepository;
import java.util.Objects;
import java.util.UUID;

final class ServiceSupport {

    private ServiceSupport() {
    }

    static Wallet requireWallet(WalletRepository repository, UUID ownerId) {
        Objects.requireNonNull(repository, "repository");
        UUID requiredOwnerId = Objects.requireNonNull(ownerId, "ownerId");
        return repository.findByOwnerId(requiredOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "wallet is not loaded for owner " + requiredOwnerId));
    }

    static long requireCategoryId(Wallet wallet, String categoryName) {
        return wallet.findCategoryId(categoryName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "category not found: " + categoryName));
    }

    static long requireCategoryId(
            Wallet wallet, String categoryName, TransactionType expectedType) {
        long categoryId = requireCategoryId(wallet, categoryName);
        TransactionType actualType = wallet.categoryType(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "category cannot be used for a manual transaction: " + categoryName));
        if (actualType != expectedType) {
            throw new ResourceNotFoundException(
                    "category has another transaction type: " + categoryName);
        }
        return categoryId;
    }
}
