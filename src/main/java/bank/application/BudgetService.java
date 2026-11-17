package bank.application;

import bank.application.exception.ApplicationException;
import bank.domain.BudgetStatus;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.WalletRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BudgetService {

    private final WalletRepository walletRepository;

    public BudgetService(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
    }

    public BudgetView setBudget(UUID ownerId, String categoryName, BigDecimal limit) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(
                wallet, categoryName, TransactionType.EXPENSE);
        if (wallet.budgets().containsKey(categoryId)) {
            throw new ApplicationException("budget already exists for category " + categoryName);
        }
        wallet.setBudget(categoryId, limit);
        walletRepository.markDirty(ownerId);
        return budgetView(wallet, categoryId);
    }

    public BudgetView editBudget(UUID ownerId, String categoryName, BigDecimal limit) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(
                wallet, categoryName, TransactionType.EXPENSE);
        requireBudget(wallet, categoryId, categoryName);
        wallet.setBudget(categoryId, limit);
        walletRepository.markDirty(ownerId);
        return budgetView(wallet, categoryId);
    }

    public void deleteBudget(UUID ownerId, String categoryName) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(
                wallet, categoryName, TransactionType.EXPENSE);
        requireBudget(wallet, categoryId, categoryName);
        wallet.removeBudget(categoryId);
        walletRepository.markDirty(ownerId);
    }

    public List<BudgetView> listBudgets(UUID ownerId) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        return wallet.budgets().keySet().stream()
                .map(categoryId -> budgetView(wallet, categoryId))
                .sorted(Comparator.comparingLong(BudgetView::categoryId))
                .toList();
    }

    private static void requireBudget(
            Wallet wallet, long categoryId, String categoryName) {
        if (!wallet.budgets().containsKey(categoryId)) {
            throw new ApplicationException(
                    "budget not found for category " + categoryName);
        }
    }

    private static BudgetView budgetView(Wallet wallet, long categoryId) {
        String categoryName = wallet.categoryName(categoryId).orElseThrow();
        BudgetStatus status = wallet.budgetStatus(categoryId);
        return new BudgetView(categoryId, categoryName, status);
    }
}
