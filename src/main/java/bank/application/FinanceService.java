package bank.application;

import bank.domain.BudgetStatus;
import bank.domain.Transaction;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FinanceService {

    private final WalletRepository walletRepository;

    public FinanceService(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
    }

    public TransactionResult addIncome(
            UUID ownerId, BigDecimal amount, String categoryName, String description) {
        return addTransaction(
                ownerId, TransactionType.INCOME, amount, categoryName, description);
    }

    public TransactionResult addIncome(
            UUID ownerId, BigDecimal amount, String description) {
        return addUncategorizedTransaction(
                ownerId, TransactionType.INCOME, amount, description);
    }

    public TransactionResult addExpense(
            UUID ownerId, BigDecimal amount, String categoryName, String description) {
        return addTransaction(
                ownerId, TransactionType.EXPENSE, amount, categoryName, description);
    }

    public TransactionResult addExpense(
            UUID ownerId, BigDecimal amount, String description) {
        return addUncategorizedTransaction(
                ownerId, TransactionType.EXPENSE, amount, description);
    }

    public List<TransactionView> transactionViews(UUID ownerId) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        Set<Long> categoryIds = new LinkedHashSet<>();
        categoryIds.add(Wallet.UNCATEGORIZED_ID);
        for (TransactionType type : TransactionType.values()) {
            categoryIds.addAll(wallet.categoryNames(type).keySet());
        }
        return categoryIds.stream()
                .flatMap(categoryId -> transactionViews(wallet, categoryId).stream())
                .sorted(Comparator.comparing(TransactionView::createdAt)
                        .thenComparing(TransactionView::id))
                .toList();
    }

    public List<TransactionView> transactionViews(UUID ownerId, String categoryName) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(wallet, categoryName);
        return transactionViews(wallet, categoryId);
    }

    private TransactionResult addTransaction(
            UUID ownerId,
            TransactionType type,
            BigDecimal amount,
            String categoryName,
            String description) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(wallet, categoryName, type);
        return addTransaction(ownerId, wallet, type, amount, categoryId, description);
    }

    private TransactionResult addUncategorizedTransaction(
            UUID ownerId,
            TransactionType type,
            BigDecimal amount,
            String description) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        return addTransaction(
                ownerId, wallet, type, amount, Wallet.UNCATEGORIZED_ID, description);
    }

    private TransactionResult addTransaction(
            UUID ownerId,
            Wallet wallet,
            TransactionType type,
            BigDecimal amount,
            long categoryId,
            String description) {
        Transaction transaction = Transaction.create(type, amount, description);
        wallet.addTransaction(categoryId, transaction);
        walletRepository.markDirty(ownerId);

        BudgetStatus budgetStatus = null;
        List<FinancialWarning> warnings = new ArrayList<>();
        if (type == TransactionType.EXPENSE) {
            BigDecimal budgetLimit = wallet.budgets().get(categoryId);
            if (budgetLimit != null) {
                budgetStatus = wallet.budgetStatus(categoryId);
                if (budgetStatus.exceeded()) {
                    warnings.add(new FinancialWarning(
                            WarningType.BUDGET_EXCEEDED,
                            wallet.categoryName(categoryId).orElseThrow(() ->
                                    new IllegalStateException(
                                            "category is missing for id " + categoryId)),
                            budgetStatus.spent(),
                            budgetStatus.limit()));
                }
            }
            addExpenseIncomeWarning(wallet, warnings);
        }

        return new TransactionResult(
                transaction, wallet.balance(), budgetStatus, warnings);
    }

    static void addExpenseIncomeWarning(
            Wallet wallet, List<FinancialWarning> warnings) {
        BigDecimal totalExpense = wallet.total(TransactionType.EXPENSE);
        BigDecimal totalIncome = wallet.total(TransactionType.INCOME);
        if (totalExpense.compareTo(totalIncome) > 0) {
            warnings.add(new FinancialWarning(
                    WarningType.EXPENSES_EXCEED_INCOME,
                    null,
                    totalExpense,
                    totalIncome));
        }
    }

    private static List<TransactionView> transactionViews(
            Wallet wallet, long categoryId) {
        String categoryName = wallet.categoryName(categoryId).orElse("category " + categoryId);
        return wallet.transactions(categoryId).stream()
                .map(transaction -> new TransactionView(
                        transaction.id(),
                        transaction.type(),
                        transaction.amount(),
                        categoryName,
                        transaction.createdAt(),
                        transaction.description().orElse(null),
                        transaction.transferId().orElse(null)))
                .toList();
    }
}
