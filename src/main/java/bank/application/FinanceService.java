package bank.application;

import bank.domain.BudgetStatus;
import bank.domain.Transaction;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public TransactionResult addExpense(
            UUID ownerId, BigDecimal amount, String categoryName, String description) {
        return addTransaction(
                ownerId, TransactionType.EXPENSE, amount, categoryName, description);
    }

    public List<Transaction> transactions(UUID ownerId) {
        return ServiceSupport.requireWallet(walletRepository, ownerId).allTransactions();
    }

    public List<Transaction> transactions(UUID ownerId, String categoryName) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(wallet, categoryName);
        return wallet.transactions(categoryId);
    }

    private TransactionResult addTransaction(
            UUID ownerId,
            TransactionType type,
            BigDecimal amount,
            String categoryName,
            String description) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        long categoryId = ServiceSupport.requireCategoryId(wallet, categoryName, type);
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
                            wallet.categoryName(categoryId).orElse(categoryName),
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
}
