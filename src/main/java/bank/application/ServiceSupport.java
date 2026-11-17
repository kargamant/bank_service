package bank.application;

import bank.application.exception.ApplicationException;
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
                .orElseThrow(() -> new ApplicationException(
                        "wallet is not loaded for owner " + requiredOwnerId));
    }

    static long requireCategoryId(Wallet wallet, String categoryName) {
        return wallet.findCategoryId(categoryName)
                .orElseThrow(() -> new ApplicationException(
                        "category not found: " + categoryName));
    }

    static long requireCategoryId(
            Wallet wallet, String categoryName, TransactionType expectedType) {
        long categoryId = requireCategoryId(wallet, categoryName);
        TransactionType actualType = wallet.categoryType(categoryId)
                .orElseThrow(() -> new ApplicationException(
                        "category cannot be used for a manual transaction: " + categoryName));
        if (actualType != expectedType) {
            throw new ApplicationException(
                    "category has another transaction type: " + categoryName);
        }
        return categoryId;
    }
}
