package bank.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import bank.domain.exception.BudgetNotFoundException;
import bank.domain.exception.CategoryNotFoundException;
import bank.domain.exception.DuplicateCategoryException;
import bank.domain.exception.DomainValidationException;

public final class Wallet {

    public static final long UNCATEGORIZED_ID = 0L;
    public static final String UNCATEGORIZED_NAME = "no category";

    private final UUID ownerId;
    private final EnumMap<TransactionType, Map<Long, String>> categoryNames;
    private final Map<Long, List<Transaction>> transactionsByCategory;
    private final Map<Long, BigDecimal> budgets;
    private long nextCategoryId;

    public Wallet(UUID ownerId) {
        this.ownerId = DomainValidation.requireNotNull(
                ownerId, "user id not specified");
        this.categoryNames = createEmptyCategoryNames();
        this.transactionsByCategory = new LinkedHashMap<>();
        this.transactionsByCategory.put(UNCATEGORIZED_ID, new ArrayList<>());
        this.budgets = new LinkedHashMap<>();
        this.nextCategoryId = 1L;
    }

    // for state recovering
    public Wallet(
            UUID ownerId,
            long nextCategoryId,
            Map<TransactionType, Map<Long, String>> categoryNames,
            Map<Long, List<Transaction>> transactionsByCategory,
            Map<Long, BigDecimal> budgets) {
        this.ownerId = DomainValidation.requireNotNull(
                ownerId, "user id not specified");
        this.categoryNames = copyCategories(categoryNames);
        this.transactionsByCategory = copyTransactions(
                transactionsByCategory, this.categoryNames);
        this.budgets = new LinkedHashMap<>(budgets);
        this.nextCategoryId = Math.max(
                nextCategoryId, calculateNextCategoryId(this.categoryNames));
    }

    public UUID ownerId() {
        return ownerId;
    }

    public long addCategory(TransactionType type, String name) {
        TransactionType requiredType = DomainValidation.requireNotNull(
                type, "category type not specified");
        String displayName = DomainValidation.normalizeCategoryName(name);
        ensureUniqueCategoryName(displayName, null);

        long categoryId = nextCategoryId++;
        categoryNames.get(requiredType).put(categoryId, displayName);
        transactionsByCategory.put(categoryId, new ArrayList<>());
        return categoryId;
    }

    public void renameCategory(TransactionType type, long categoryId, String newName) {
        ensureMutableCategoryId(categoryId);
        TransactionType requiredType = DomainValidation.requireNotNull(
                type, "category type not specified");
        Map<Long, String> names = categoryNames.get(requiredType);
        if (!names.containsKey(categoryId)) {
            throw categoryNotFound(categoryId);
        }

        String displayName = DomainValidation.normalizeCategoryName(newName);
        ensureUniqueCategoryName(displayName, categoryId);
        names.put(categoryId, displayName);
    }

    public void removeCategory(TransactionType type, long categoryId) {
        ensureMutableCategoryId(categoryId);
        TransactionType requiredType = DomainValidation.requireNotNull(
                type, "category type not specified");
        Map<Long, String> names = categoryNames.get(requiredType);
        if (names.remove(categoryId) == null) {
            throw categoryNotFound(categoryId);
        }

        List<Transaction> removedTransactions = transactionsByCategory.remove(categoryId);
        if (removedTransactions != null) {
            transactionsByCategory.get(UNCATEGORIZED_ID).addAll(removedTransactions);
        }
        budgets.remove(categoryId);
    }

    public OptionalLong findCategoryId(String name) {
        String normalizedName = DomainValidation.categoryNameKey(name);
        if (DomainValidation.categoryNameKey(UNCATEGORIZED_NAME).equals(normalizedName)) {
            return OptionalLong.of(UNCATEGORIZED_ID);
        }
        for (Map<Long, String> names : categoryNames.values()) {
            for (Map.Entry<Long, String> entry : names.entrySet()) {
                if (DomainValidation.categoryNameKey(entry.getValue()).equals(normalizedName)) {
                    return OptionalLong.of(entry.getKey());
                }
            }
        }
        return OptionalLong.empty();
    }

    public Optional<String> categoryName(long categoryId) {
        if (categoryId == UNCATEGORIZED_ID) {
            return Optional.of(UNCATEGORIZED_NAME);
        }
        for (Map<Long, String> names : categoryNames.values()) {
            String name = names.get(categoryId);
            if (name != null) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    public Optional<TransactionType> categoryType(long categoryId) {
        if (categoryId == UNCATEGORIZED_ID) {
            return Optional.empty();
        }
        for (TransactionType type : TransactionType.values()) {
            if (categoryNames.get(type).containsKey(categoryId)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public Map<Long, String> categoryNames(TransactionType type) {
        TransactionType requiredType = DomainValidation.requireNotNull(
                type, "category type not specified");
        return Collections.unmodifiableMap(new LinkedHashMap<>(categoryNames.get(requiredType)));
    }

    public void addTransaction(long categoryId, Transaction transaction) {
        Transaction requiredTransaction = DomainValidation.requireNotNull(
                transaction, "no operation specified");
        List<Transaction> categoryTransactions = transactionsByCategory.get(categoryId);
        if (categoryTransactions == null) {
            throw categoryNotFound(categoryId);
        }

        if (categoryId != UNCATEGORIZED_ID) {
            TransactionType type = categoryType(categoryId).orElseThrow(
                    () -> categoryNotFound(categoryId));
            if (type != requiredTransaction.type()) {
                throw new DomainValidationException(
                        "transaction type does not match category type");
            }
        }

        categoryTransactions.add(requiredTransaction);
    }

    public List<Transaction> transactions(long categoryId) {
        List<Transaction> transactions = transactionsByCategory.get(categoryId);
        if (transactions == null) {
            throw categoryNotFound(categoryId);
        }
        return List.copyOf(transactions);
    }

    public List<Transaction> allTransactions() {
        return transactionsByCategory.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Transaction::createdAt)
                        .thenComparing(Transaction::id))
                .toList();
    }

    public void setBudget(long categoryId, BigDecimal limit) {
        ensureExpenseCategory(categoryId);
        budgets.put(categoryId,
                DomainValidation.requirePositiveMoney(limit, "budget limit"));
    }

    public void removeBudget(long categoryId) {
        if (budgets.remove(categoryId) == null) {
            throw new BudgetNotFoundException(
                    "budget for category id " + categoryId + " not found");
        }
    }

    public Map<Long, BigDecimal> budgets() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(budgets));
    }

    public BigDecimal total(TransactionType type) {
        TransactionType requiredType = DomainValidation.requireNotNull(
                type, "transaction type is not specified");
        return transactionsByCategory.values().stream()
                .flatMap(Collection::stream)
                .filter(transaction -> transaction.type() == requiredType)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal total(long categoryId, TransactionType type) {
        TransactionType requiredType = DomainValidation.requireNotNull(
                type, "transaction type is not specified");
        return transactions(categoryId).stream()
                .filter(transaction -> transaction.type() == requiredType)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal balance() {
        return total(TransactionType.INCOME).subtract(total(TransactionType.EXPENSE));
    }

    public BudgetStatus budgetStatus(long categoryId) {
        BigDecimal limit = budgets.get(categoryId);
        if (limit == null) {
            throw new BudgetNotFoundException(
                    "budget for category id " + categoryId + " not found");
        }
        BigDecimal spent = total(categoryId, TransactionType.EXPENSE);
        return new BudgetStatus(limit, spent);
    }

    private static EnumMap<TransactionType, Map<Long, String>> createEmptyCategoryNames() {
        EnumMap<TransactionType, Map<Long, String>> result =
                new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            result.put(type, new LinkedHashMap<>());
        }
        return result;
    }

    private static EnumMap<TransactionType, Map<Long, String>> copyCategories(
            Map<TransactionType, Map<Long, String>> source) {
        EnumMap<TransactionType, Map<Long, String>> result = createEmptyCategoryNames();
        for (TransactionType type : TransactionType.values()) {
            Map<Long, String> sourceNames = source.getOrDefault(type, Map.of());
            if (sourceNames != null) {
                result.get(type).putAll(sourceNames);
            }
        }
        return result;
    }

    private static Map<Long, List<Transaction>> copyTransactions(
            Map<Long, List<Transaction>> source,
            EnumMap<TransactionType, Map<Long, String>> categories) {
        Map<Long, List<Transaction>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Transaction>> entry : source.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        result.putIfAbsent(UNCATEGORIZED_ID, new ArrayList<>());
        categories.values().stream()
                .flatMap(names -> names.keySet().stream())
                .forEach(categoryId -> result.putIfAbsent(categoryId, new ArrayList<>()));
        return result;
    }

    private static long calculateNextCategoryId(
            EnumMap<TransactionType, Map<Long, String>> categories) {
        long maximumId = categories.values().stream()
                .flatMap(names -> names.keySet().stream())
                .mapToLong(Long::longValue)
                .max()
                .orElse(UNCATEGORIZED_ID);
        return maximumId + 1;
    }

    private void ensureUniqueCategoryName(String displayName, Long ignoredCategoryId) {
        String nameKey = DomainValidation.categoryNameKey(displayName);
        if (DomainValidation.categoryNameKey(UNCATEGORIZED_NAME).equals(nameKey)) {
            throw new DuplicateCategoryException(
                    "name \"" + UNCATEGORIZED_NAME + "\" is reserved by system");
        }
        for (Map<Long, String> names : categoryNames.values()) {
            for (Map.Entry<Long, String> entry : names.entrySet()) {
                if ((ignoredCategoryId == null || !entry.getKey().equals(ignoredCategoryId))
                        && DomainValidation.categoryNameKey(entry.getValue()).equals(nameKey)) {
                    throw new DuplicateCategoryException(
                            "category named \"" + displayName + "\" already exists");
                }
            }
        }
    }

    private void ensureMutableCategoryId(long categoryId) {
        if (categoryId == UNCATEGORIZED_ID) {
            throw new DomainValidationException(
                    "system default category " + UNCATEGORIZED_NAME + " cannot be changed");
        }
    }

    private void ensureExpenseCategory(long categoryId) {
        if (categoryId == UNCATEGORIZED_ID
                || !categoryNames.get(TransactionType.EXPENSE).containsKey(categoryId)) {
            throw new DomainValidationException(
                    "budget can only be set for an existing expense category");
        }
    }

    private static CategoryNotFoundException categoryNotFound(long categoryId) {
        return new CategoryNotFoundException(
                "category with id " + categoryId + " not found");
    }
}
