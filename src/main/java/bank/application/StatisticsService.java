package bank.application;

import bank.domain.BudgetStatus;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class StatisticsService {

    private final WalletRepository walletRepository;

    public StatisticsService(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
    }

    public FinancialSummary summarize(UUID ownerId) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        Set<Long> categoryIds = new LinkedHashSet<>();
        for (TransactionType type : TransactionType.values()) {
            categoryIds.addAll(wallet.categoryNames(type).keySet());
        }
        if (!wallet.transactions(Wallet.UNCATEGORIZED_ID).isEmpty()) {
            categoryIds.add(Wallet.UNCATEGORIZED_ID);
        }
        return buildSummary(wallet, categoryIds, List.of(), List.of());
    }

    public FinancialSummary summarizeCategories(
            UUID ownerId, Collection<String> categoryNames) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        Objects.requireNonNull(categoryNames, "categoryNames");

        Set<Long> categoryIds = new LinkedHashSet<>();
        List<String> missingCategories = new ArrayList<>();
        List<String> categoriesWithoutData = new ArrayList<>();

        for (String requestedName : categoryNames) {
            if (requestedName == null || requestedName.isBlank()) {
                missingCategories.add(String.valueOf(requestedName));
                continue;
            }
            var categoryId = wallet.findCategoryId(requestedName);
            if (categoryId.isEmpty()) {
                missingCategories.add(requestedName);
                continue;
            }
            long id = categoryId.getAsLong();
            if (categoryIds.add(id) && wallet.transactions(id).isEmpty()) {
                categoriesWithoutData.add(wallet.categoryName(id).orElse(requestedName));
            }
        }

        return buildSummary(
                wallet, categoryIds, missingCategories, categoriesWithoutData);
    }

    private static FinancialSummary buildSummary(
            Wallet wallet,
            Collection<Long> categoryIds,
            List<String> missingCategories,
            List<String> categoriesWithoutData) {
        Map<String, BigDecimal> incomeByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        Map<String, BudgetStatus> budgetStatuses = new LinkedHashMap<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Long categoryId : categoryIds) {
            String categoryName = wallet.categoryName(categoryId)
                    .orElse("category " + categoryId);
            BigDecimal income = wallet.total(categoryId, TransactionType.INCOME);
            BigDecimal expense = wallet.total(categoryId, TransactionType.EXPENSE);

            if (income.signum() != 0) {
                incomeByCategory.put(categoryName, income);
                totalIncome = totalIncome.add(income);
            }
            if (expense.signum() != 0) {
                expenseByCategory.put(categoryName, expense);
                totalExpense = totalExpense.add(expense);
            }
            if (wallet.budgets().containsKey(categoryId)) {
                budgetStatuses.put(categoryName, wallet.budgetStatus(categoryId));
            }
        }

        return new FinancialSummary(
                totalIncome,
                totalExpense,
                incomeByCategory,
                expenseByCategory,
                budgetStatuses,
                missingCategories,
                categoriesWithoutData);
    }
}
