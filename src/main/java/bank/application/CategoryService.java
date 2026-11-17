package bank.application;

import bank.application.exception.ApplicationException;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.WalletRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CategoryService {

    private final WalletRepository walletRepository;

    public CategoryService(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
    }

    public CategoryView addCategory(UUID ownerId, TransactionType type, String name) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = wallet.addCategory(type, name);
        walletRepository.markDirty(ownerId);
        return categoryView(wallet, type, categoryId);
    }

    public List<CategoryView> listCategories(UUID ownerId, TransactionType type) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        return wallet.categoryNames(type).keySet().stream()
                .map(categoryId -> categoryView(wallet, type, categoryId))
                .sorted(Comparator.comparingLong(CategoryView::id))
                .toList();
    }

    public List<CategoryView> listCategories(UUID ownerId) {
        List<CategoryView> result = new ArrayList<>();
        for (TransactionType type : TransactionType.values()) {
            result.addAll(listCategories(ownerId, type));
        }
        result.sort(Comparator.comparingLong(CategoryView::id));
        return List.copyOf(result);
    }

    public CategoryView renameCategory(UUID ownerId, String currentName, String newName) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(wallet, currentName);
        TransactionType type = wallet.categoryType(categoryId)
                .orElseThrow(() -> new ApplicationException(
                        "system category cannot be renamed"));
        wallet.renameCategory(type, categoryId, newName);
        walletRepository.markDirty(ownerId);
        return categoryView(wallet, type, categoryId);
    }

    public void deleteCategory(UUID ownerId, String categoryName) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(wallet, categoryName);
        TransactionType type = wallet.categoryType(categoryId)
                .orElseThrow(() -> new ApplicationException(
                        "system category cannot be deleted"));
        wallet.removeCategory(type, categoryId);
        walletRepository.markDirty(ownerId);
    }

    private static CategoryView categoryView(
            Wallet wallet, TransactionType type, long categoryId) {
        String name = wallet.categoryName(categoryId).orElseThrow();
        int transactionCount = wallet.transactions(categoryId).size();
        Map<Long, java.math.BigDecimal> budgets = wallet.budgets();
        return new CategoryView(
                categoryId, name, type, transactionCount, budgets.containsKey(categoryId));
    }
}
