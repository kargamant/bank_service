package bank.cli;

import bank.application.AuthService;
import bank.application.BudgetService;
import bank.application.BudgetView;
import bank.application.CategoryService;
import bank.application.CategoryView;
import bank.application.FinanceService;
import bank.application.FinancialSummary;
import bank.application.FinancialWarning;
import bank.application.ImportExportService;
import bank.application.PersistenceService;
import bank.application.Session;
import bank.application.StatisticsService;
import bank.application.TransactionResult;
import bank.application.TransactionView;
import bank.application.TransferResult;
import bank.application.TransferService;
import bank.application.WarningType;
import bank.application.exception.ApplicationException;
import bank.application.exception.AuthorizationException;
import bank.domain.BudgetStatus;
import bank.domain.TransactionType;
import bank.domain.exception.DomainException;
import bank.ports.StorageException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CommandHandler {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuthService authService;
    private final CategoryService categoryService;
    private final FinanceService financeService;
    private final BudgetService budgetService;
    private final StatisticsService statisticsService;
    private final TransferService transferService;
    private final ImportExportService importExportService;
    private final PersistenceService persistenceService;
    private final Session session;
    private final TablePrinter tablePrinter;
    private final PrintStream out;

    public CommandHandler(
            AuthService authService,
            CategoryService categoryService,
            FinanceService financeService,
            BudgetService budgetService,
            StatisticsService statisticsService,
            TransferService transferService,
            ImportExportService importExportService,
            PersistenceService persistenceService,
            Session session,
            TablePrinter tablePrinter,
            PrintStream out) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService");
        this.financeService = Objects.requireNonNull(financeService, "financeService");
        this.budgetService = Objects.requireNonNull(budgetService, "budgetService");
        this.statisticsService = Objects.requireNonNull(
                statisticsService, "statisticsService");
        this.transferService = Objects.requireNonNull(transferService, "transferService");
        this.importExportService = Objects.requireNonNull(
                importExportService, "importExportService");
        this.persistenceService = Objects.requireNonNull(
                persistenceService, "persistenceService");
        this.session = Objects.requireNonNull(session, "session");
        this.tablePrinter = Objects.requireNonNull(tablePrinter, "tablePrinter");
        this.out = Objects.requireNonNull(out, "out");
    }

    public boolean handle(ParsedCommand command) {
        Objects.requireNonNull(command, "command");
        try {
            return dispatch(command);
        } catch (CommandSyntaxException
                | ApplicationException
                | DomainException
                | StorageException exception) {
            out.println("Error: " + exception.getMessage());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            out.println("Error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            out.println("Unexpected error: " + exception.getMessage());
        }
        return true;
    }

    private boolean dispatch(ParsedCommand command) {
        return switch (command.name()) {
            case "help" -> handleHelp(command.tokens());
            case "register" -> handleRegister(command.tokens());
            case "login" -> handleLogin(command.tokens());
            case "logout" -> handleLogout(command.tokens());
            case "save" -> handleSave(command.tokens());
            case "category" -> handleCategory(command.tokens());
            case "income" -> handleTransaction(command.tokens(), TransactionType.INCOME);
            case "expense" -> handleTransaction(command.tokens(), TransactionType.EXPENSE);
            case "transactions" -> handleTransactions(command.tokens());
            case "budget" -> handleBudget(command.tokens());
            case "summary" -> handleSummary(command.tokens());
            case "transfer" -> handleTransfer(command.tokens());
            case "export" -> handleExport(command.tokens());
            case "import" -> handleImport(command.tokens());
            case "exit" -> handleExit(command.tokens());
            default -> throw new CommandSyntaxException(
                    "unknown command: " + command.name() + ". Use help.");
        };
    }

    private boolean handleHelp(List<String> tokens) {
        requireRange(tokens, 1, 2, "help [command]");
        out.println(tokens.size() == 1 ? CommandHelp.general() : CommandHelp.topic(tokens.get(1)));
        return true;
    }

    private boolean handleRegister(List<String> tokens) {
        requireSize(tokens, 3, "register <login> <password>");
        requireGuest();
        char[] password = tokens.get(2).toCharArray();
        try {
            var user = authService.register(tokens.get(1), password);
            out.println("User registered: " + user.login());
            autoSave();
        } finally {
            Arrays.fill(password, '\0');
        }
        return true;
    }

    private boolean handleLogin(List<String> tokens) {
        requireSize(tokens, 3, "login <login> <password>");
        char[] password = tokens.get(2).toCharArray();
        try {
            authService.login(tokens.get(1), password);
            out.println("Signed in as " + tokens.get(1));
        } finally {
            Arrays.fill(password, '\0');
        }
        return true;
    }

    private boolean handleLogout(List<String> tokens) {
        requireSize(tokens, 1, "logout");
        authService.logout();
        out.println("Signed out. Data saved.");
        return true;
    }

    private boolean handleSave(List<String> tokens) {
        requireSize(tokens, 1, "save");
        currentUserId();
        persistenceService.saveDirtyState();
        out.println("Data saved.");
        return true;
    }

    private boolean handleCategory(List<String> tokens) {
        requireMinimum(tokens, 2, "category add|list|rename|delete ...");
        UUID ownerId = currentUserId();
        String action = lower(tokens.get(1));
        switch (action) {
            case "add" -> {
                requireSize(tokens, 4, "category add <income|expense> <name>");
                CategoryView category = categoryService.addCategory(
                        ownerId, parseType(tokens.get(2)), tokens.get(3));
                out.println("Category created: " + category.name());
                autoSave();
            }
            case "list" -> {
                requireRange(tokens, 2, 3, "category list [income|expense]");
                List<CategoryView> categories = tokens.size() == 2
                        ? categoryService.listCategories(ownerId)
                        : categoryService.listCategories(ownerId, parseType(tokens.get(2)));
                printCategories(categories);
            }
            case "rename" -> {
                requireSize(tokens, 4, "category rename <current-name> <new-name>");
                CategoryView category = categoryService.renameCategory(
                        ownerId, tokens.get(2), tokens.get(3));
                out.println("Category renamed to: " + category.name());
                autoSave();
            }
            case "delete" -> {
                requireSize(tokens, 3, "category delete <name>");
                categoryService.deleteCategory(ownerId, tokens.get(2));
                out.println("Category deleted: " + tokens.get(2));
                autoSave();
            }
            default -> throw new CommandSyntaxException(
                    "unknown category action. Use help category.");
        }
        return true;
    }

    private boolean handleTransaction(List<String> tokens, TransactionType type) {
        String command = lower(tokens.getFirst());
        requireRange(tokens, 4, 5,
                command + " add <amount> <category> [description]");
        if (!"add".equals(lower(tokens.get(1)))) {
            throw new CommandSyntaxException(
                    command + " supports only the add action");
        }
        UUID ownerId = currentUserId();
        BigDecimal amount = parseAmount(tokens.get(2));
        String description = tokens.size() == 5 ? tokens.get(4) : null;
        TransactionResult result = type == TransactionType.INCOME
                ? financeService.addIncome(ownerId, amount, tokens.get(3), description)
                : financeService.addExpense(ownerId, amount, tokens.get(3), description);
        out.println("Transaction added: " + result.transaction().id());
        out.println("Balance: " + TablePrinter.money(result.balance()));
        result.optionalBudgetStatus().ifPresent(status -> out.println(
                "Budget remaining: " + TablePrinter.money(status.remaining())));
        printWarnings(result.warnings());
        autoSave();
        return true;
    }

    private boolean handleTransactions(List<String> tokens) {
        requireRange(tokens, 1, 2, "transactions [category]");
        UUID ownerId = currentUserId();
        List<TransactionView> transactions = tokens.size() == 1
                ? financeService.transactionViews(ownerId)
                : financeService.transactionViews(ownerId, tokens.get(1));
        printTransactions(transactions);
        return true;
    }

    private boolean handleBudget(List<String> tokens) {
        requireMinimum(tokens, 2, "budget set|edit|delete|list ...");
        UUID ownerId = currentUserId();
        switch (lower(tokens.get(1))) {
            case "set" -> {
                requireSize(tokens, 4, "budget set <category> <amount>");
                BudgetView budget = budgetService.setBudget(
                        ownerId, tokens.get(2), parseAmount(tokens.get(3)));
                printBudget(budget);
                autoSave();
            }
            case "edit" -> {
                requireSize(tokens, 4, "budget edit <category> <amount>");
                BudgetView budget = budgetService.editBudget(
                        ownerId, tokens.get(2), parseAmount(tokens.get(3)));
                printBudget(budget);
                autoSave();
            }
            case "delete" -> {
                requireSize(tokens, 3, "budget delete <category>");
                budgetService.deleteBudget(ownerId, tokens.get(2));
                out.println("Budget deleted: " + tokens.get(2));
                autoSave();
            }
            case "list" -> {
                requireSize(tokens, 2, "budget list");
                printBudgets(budgetService.listBudgets(ownerId));
            }
            default -> throw new CommandSyntaxException(
                    "unknown budget action. Use help budget.");
        }
        return true;
    }

    private boolean handleSummary(List<String> tokens) {
        UUID ownerId = currentUserId();
        FinancialSummary summary;
        if (tokens.size() == 1) {
            summary = statisticsService.summarize(ownerId);
        } else {
            requireMinimum(tokens, 3, "summary categories <category> [category ...]");
            if (!"categories".equals(lower(tokens.get(1)))) {
                throw new CommandSyntaxException(
                        "usage: summary categories <category> [category ...]");
            }
            summary = statisticsService.summarizeCategories(ownerId, tokens.subList(2, tokens.size()));
        }
        printSummary(summary);
        return true;
    }

    private boolean handleTransfer(List<String> tokens) {
        requireRange(tokens, 3, 4,
                "transfer <recipient-login> <amount> [description]");
        String description = tokens.size() == 4 ? tokens.get(3) : null;
        TransferResult result = transferService.transfer(
                currentUserId(), tokens.get(1), parseAmount(tokens.get(2)), description);
        out.println("Transfer completed: " + result.transferId());
        out.println("Sender balance: " + TablePrinter.money(result.senderBalance()));
        printWarnings(result.warnings());
        autoSave();
        return true;
    }

    private boolean handleExport(List<String> tokens) {
        requireSize(tokens, 3, "export csv|json <path>");
        UUID ownerId = currentUserId();
        Path destination = Path.of(tokens.get(2));
        switch (lower(tokens.get(1))) {
            case "csv" -> importExportService.exportCsv(ownerId, destination);
            case "json" -> importExportService.exportJson(ownerId, destination);
            default -> throw new CommandSyntaxException("export format must be csv or json");
        }
        out.println("Exported to: " + destination.toAbsolutePath().normalize());
        return true;
    }

    private boolean handleImport(List<String> tokens) {
        requireSize(tokens, 3, "import json <path>");
        if (!"json".equals(lower(tokens.get(1)))) {
            throw new CommandSyntaxException("only JSON snapshots can be imported");
        }
        Path source = Path.of(tokens.get(2));
        importExportService.importJson(currentUserId(), source);
        out.println("Snapshot imported: " + source.toAbsolutePath().normalize());
        autoSave();
        return true;
    }

    private boolean handleExit(List<String> tokens) {
        requireSize(tokens, 1, "exit");
        persistenceService.saveDirtyState();
        out.println("Data saved. Goodbye.");
        return false;
    }

    private void printCategories(List<CategoryView> categories) {
        List<List<String>> rows = categories.stream()
                .map(category -> List.of(
                        Long.toString(category.id()),
                        category.name(),
                        lower(category.type().name()),
                        Integer.toString(category.transactionCount()),
                        category.hasBudget() ? "yes" : "no"))
                .toList();
        tablePrinter.print(
                List.of("ID", "Name", "Type", "Transactions", "Budget"), rows);
    }

    private void printTransactions(List<TransactionView> transactions) {
        List<List<String>> rows = transactions.stream()
                .map(transaction -> List.of(
                        transaction.createdAt().format(DATE_TIME_FORMAT),
                        lower(transaction.type().name()),
                        TablePrinter.money(transaction.amount()),
                        transaction.category(),
                        transaction.optionalDescription().orElse(""),
                        transaction.optionalTransferId().map(Object::toString).orElse("")))
                .toList();
        tablePrinter.print(
                List.of("Date", "Type", "Amount", "Category", "Description", "Transfer"),
                rows);
    }

    private void printBudget(BudgetView budget) {
        printBudgets(List.of(budget));
    }

    private void printBudgets(List<BudgetView> budgets) {
        List<List<String>> rows = budgets.stream()
                .map(budget -> budgetRow(budget.categoryName(), budget.status()))
                .toList();
        tablePrinter.print(List.of("Category", "Limit", "Spent", "Remaining"), rows);
    }

    private void printSummary(FinancialSummary summary) {
        out.println("Total income:  " + TablePrinter.money(summary.totalIncome()));
        out.println("Total expense: " + TablePrinter.money(summary.totalExpense()));
        out.println("Balance:       " + TablePrinter.money(summary.balance()));

        out.println("Income by category:");
        printAmountMap(summary.incomeByCategory());
        out.println("Expense by category:");
        printAmountMap(summary.expenseByCategory());
        out.println("Budgets:");
        List<List<String>> budgetRows = summary.budgetStatuses().entrySet().stream()
                .map(entry -> budgetRow(entry.getKey(), entry.getValue()))
                .toList();
        tablePrinter.print(List.of("Category", "Limit", "Spent", "Remaining"), budgetRows);

        for (String category : summary.missingCategories()) {
            out.println("Warning: category not found: " + category);
        }
        for (String category : summary.categoriesWithoutData()) {
            out.println("Warning: no transaction data for category: " + category);
        }
    }

    private void printAmountMap(Map<String, BigDecimal> values) {
        List<List<String>> rows = values.entrySet().stream()
                .map(entry -> List.of(entry.getKey(), TablePrinter.money(entry.getValue())))
                .toList();
        tablePrinter.print(List.of("Category", "Amount"), rows);
    }

    private static List<String> budgetRow(String categoryName, BudgetStatus status) {
        return List.of(
                categoryName,
                TablePrinter.money(status.limit()),
                TablePrinter.money(status.spent()),
                TablePrinter.money(status.remaining()));
    }

    private void printWarnings(List<FinancialWarning> warnings) {
        for (FinancialWarning warning : warnings) {
            if (warning.type() == WarningType.BUDGET_EXCEEDED) {
                out.println("Warning: budget exceeded for "
                        + warning.categoryName().orElse("category")
                        + ": spent " + TablePrinter.money(warning.actual())
                        + ", limit " + TablePrinter.money(warning.limit()));
            } else if (warning.type() == WarningType.EXPENSES_EXCEED_INCOME) {
                out.println("Warning: expenses " + TablePrinter.money(warning.actual())
                        + " exceed income " + TablePrinter.money(warning.limit()));
            }
        }
    }

    private void autoSave() {
        try {
            persistenceService.saveDirtyState();
        } catch (StorageException exception) {
            out.println("Warning: changes remain in memory but were not saved: "
                    + exception.getMessage());
        }
    }

    private UUID currentUserId() {
        return session.requireUserId();
    }

    private void requireGuest() {
        if (session.isAuthenticated()) {
            throw new AuthorizationException("logout before registering another user");
        }
    }

    private static TransactionType parseType(String value) {
        return switch (lower(value)) {
            case "income" -> TransactionType.INCOME;
            case "expense" -> TransactionType.EXPENSE;
            default -> throw new CommandSyntaxException("type must be income or expense");
        };
    }

    private static BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new CommandSyntaxException("invalid amount: " + value);
        }
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static void requireSize(List<String> tokens, int expected, String usage) {
        if (tokens.size() != expected) {
            throw new CommandSyntaxException("usage: " + usage);
        }
    }

    private static void requireRange(
            List<String> tokens, int minimum, int maximum, String usage) {
        if (tokens.size() < minimum || tokens.size() > maximum) {
            throw new CommandSyntaxException("usage: " + usage);
        }
    }

    private static void requireMinimum(List<String> tokens, int minimum, String usage) {
        if (tokens.size() < minimum) {
            throw new CommandSyntaxException("usage: " + usage);
        }
    }
}
