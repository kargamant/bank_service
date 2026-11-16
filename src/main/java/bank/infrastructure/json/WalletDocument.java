package bank.infrastructure.json;

import bank.domain.BudgetStatus;
import bank.domain.Transaction;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

record WalletDocument(
        UUID ownerId,
        long nextCategoryId,
        Map<TransactionType, Map<Long, String>> categoryNames,
        Map<Long, List<TransactionData>> transactionsByCategory,
        Map<Long, BigDecimal> budgets) {

    static WalletDocument fromDomain(Wallet wallet) {
        EnumMap<TransactionType, Map<Long, String>> categories =
                new EnumMap<>(TransactionType.class);
        Set<Long> categoryIds = new LinkedHashSet<>();
        categoryIds.add(Wallet.UNCATEGORIZED_ID);

        for (TransactionType type : TransactionType.values()) {
            Map<Long, String> names = new LinkedHashMap<>(wallet.categoryNames(type));
            categories.put(type, names);
            categoryIds.addAll(names.keySet());
        }

        Map<Long, List<TransactionData>> transactions = new LinkedHashMap<>();
        for (Long categoryId : categoryIds) {
            List<TransactionData> transactionData = wallet.transactions(categoryId).stream()
                    .map(TransactionData::fromDomain)
                    .toList();
            transactions.put(categoryId, transactionData);
        }

        return new WalletDocument(
                wallet.ownerId(),
                wallet.nextCategoryId(),
                categories,
                transactions,
                new LinkedHashMap<>(wallet.budgets()));
    }

    Wallet toDomain() {
        if (categoryNames == null || transactionsByCategory == null || budgets == null) {
            throw new IllegalArgumentException("wallet JSON contains null collections");
        }

        Map<Long, TransactionType> categoryTypes = validateCategories();
        Map<Long, List<Transaction>> transactions = new LinkedHashMap<>();
        Set<UUID> transactionIds = new HashSet<>();
        for (Map.Entry<Long, List<TransactionData>> entry
                : transactionsByCategory.entrySet()) {
            Long categoryId = entry.getKey();
            if (categoryId == null
                    || (categoryId != Wallet.UNCATEGORIZED_ID
                    && !categoryTypes.containsKey(categoryId))) {
                throw new IllegalArgumentException(
                        "wallet JSON contains transactions for an unknown category");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "wallet JSON contains a null transaction list");
            }
            List<Transaction> domainTransactions = new ArrayList<>();
            for (TransactionData transaction : entry.getValue()) {
                if (transaction == null) {
                    throw new IllegalArgumentException(
                            "wallet JSON contains a null transaction");
                }
                Transaction domainTransaction = transaction.toDomain();
                if (!transactionIds.add(domainTransaction.id())) {
                    throw new IllegalArgumentException(
                            "wallet JSON contains duplicate transaction id "
                                    + domainTransaction.id());
                }
                TransactionType categoryType = categoryTypes.get(categoryId);
                if (categoryType != null && categoryType != domainTransaction.type()) {
                    throw new IllegalArgumentException(
                            "transaction type does not match category " + categoryId);
                }
                domainTransactions.add(domainTransaction);
            }
            transactions.put(categoryId, domainTransactions);
        }

        validateBudgets(categoryTypes);

        return new Wallet(
                ownerId,
                nextCategoryId,
                categoryNames,
                transactions,
                budgets);
    }

    private Map<Long, TransactionType> validateCategories() {
        Map<Long, TransactionType> categoryTypes = new LinkedHashMap<>();
        Set<String> normalizedNames = new HashSet<>();
        String reservedName = normalizeCategoryName(Wallet.UNCATEGORIZED_NAME);

        for (TransactionType type : TransactionType.values()) {
            Map<Long, String> names = categoryNames.getOrDefault(type, Map.of());
            if (names == null) {
                throw new IllegalArgumentException("wallet JSON contains a null category map");
            }
            for (Map.Entry<Long, String> entry : names.entrySet()) {
                Long categoryId = entry.getKey();
                if (categoryId == null || categoryId <= Wallet.UNCATEGORIZED_ID) {
                    throw new IllegalArgumentException(
                            "wallet JSON contains an invalid category id");
                }
                if (categoryTypes.putIfAbsent(categoryId, type) != null) {
                    throw new IllegalArgumentException(
                            "wallet JSON contains duplicate category id " + categoryId);
                }
                String normalizedName = normalizeCategoryName(entry.getValue());
                if (reservedName.equals(normalizedName)
                        || !normalizedNames.add(normalizedName)) {
                    throw new IllegalArgumentException(
                            "wallet JSON contains duplicate or reserved category name");
                }
            }
        }
        return categoryTypes;
    }

    private void validateBudgets(Map<Long, TransactionType> categoryTypes) {
        for (Map.Entry<Long, BigDecimal> entry : budgets.entrySet()) {
            Long categoryId = entry.getKey();
            if (categoryId == null
                    || categoryTypes.get(categoryId) != TransactionType.EXPENSE) {
                throw new IllegalArgumentException(
                        "wallet JSON contains a budget for a non-expense category");
            }
            new BudgetStatus(entry.getValue(), BigDecimal.ZERO);
        }
    }

    private static String normalizeCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("wallet JSON contains an empty category name");
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
