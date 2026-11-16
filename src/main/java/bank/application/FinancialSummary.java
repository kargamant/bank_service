package bank.application;

import bank.domain.BudgetStatus;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FinancialSummary {

    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal balance;
    private final Map<String, BigDecimal> incomeByCategory;
    private final Map<String, BigDecimal> expenseByCategory;
    private final Map<String, BudgetStatus> budgetStatuses;
    private final List<String> missingCategories;
    private final List<String> categoriesWithoutData;

    public FinancialSummary(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            Map<String, BigDecimal> incomeByCategory,
            Map<String, BigDecimal> expenseByCategory,
            Map<String, BudgetStatus> budgetStatuses,
            List<String> missingCategories,
            List<String> categoriesWithoutData) {
        this.totalIncome = Objects.requireNonNull(totalIncome, "totalIncome");
        this.totalExpense = Objects.requireNonNull(totalExpense, "totalExpense");
        this.balance = totalIncome.subtract(totalExpense);
        this.incomeByCategory = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(incomeByCategory, "incomeByCategory")));
        this.expenseByCategory = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(expenseByCategory, "expenseByCategory")));
        this.budgetStatuses = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(budgetStatuses, "budgetStatuses")));
        this.missingCategories = List.copyOf(
                Objects.requireNonNull(missingCategories, "missingCategories"));
        this.categoriesWithoutData = List.copyOf(
                Objects.requireNonNull(categoriesWithoutData, "categoriesWithoutData"));
    }

    public BigDecimal totalIncome() {
        return totalIncome;
    }

    public BigDecimal totalExpense() {
        return totalExpense;
    }

    public BigDecimal balance() {
        return balance;
    }

    public Map<String, BigDecimal> incomeByCategory() {
        return incomeByCategory;
    }

    public Map<String, BigDecimal> expenseByCategory() {
        return expenseByCategory;
    }

    public Map<String, BudgetStatus> budgetStatuses() {
        return budgetStatuses;
    }

    public List<String> missingCategories() {
        return missingCategories;
    }

    public List<String> categoriesWithoutData() {
        return categoriesWithoutData;
    }
}
