package bank.infrastructure.export;

import bank.application.FinancialSummary;
import bank.domain.BudgetStatus;
import bank.domain.Transaction;
import bank.domain.TransactionType;
import bank.domain.Wallet;
import bank.ports.ReportExporter;
import bank.ports.StorageException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CsvReportExporter implements ReportExporter {

    @Override
    public void export(Path destination, Wallet wallet, FinancialSummary summary) {
        Path absoluteDestination = Objects.requireNonNull(destination, "destination")
                .toAbsolutePath().normalize();
        Objects.requireNonNull(wallet, "wallet");
        Objects.requireNonNull(summary, "summary");

        try {
            Files.createDirectories(absoluteDestination.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(
                    absoluteDestination, StandardCharsets.UTF_8)) {
                writeSummary(writer, summary);
                writer.newLine();
                writeTransactions(writer, wallet);
                writer.newLine();
                writeBudgets(writer, summary);
            }
        } catch (IOException exception) {
            throw new StorageException(
                    "cannot write CSV report " + absoluteDestination, exception);
        }
    }

    private static void writeSummary(BufferedWriter writer, FinancialSummary summary)
            throws IOException {
        writeRow(writer, "metric", "value");
        writeRow(writer, "total_income", money(summary.totalIncome()));
        writeRow(writer, "total_expense", money(summary.totalExpense()));
        writeRow(writer, "balance", money(summary.balance()));
    }

    private static void writeTransactions(BufferedWriter writer, Wallet wallet)
            throws IOException {
        writeRow(
                writer,
                "transaction_id",
                "created_at",
                "type",
                "category",
                "amount",
                "description",
                "transfer_id");

        Map<Long, String> categories = categoryNames(wallet);
        for (Map.Entry<Long, String> category : categories.entrySet()) {
            for (Transaction transaction : wallet.transactions(category.getKey())) {
                writeRow(
                        writer,
                        transaction.id().toString(),
                        transaction.createdAt().toString(),
                        transaction.type().name(),
                        category.getValue(),
                        money(transaction.amount()),
                        transaction.description().orElse(""),
                        transaction.transferId().map(Object::toString).orElse(""));
            }
        }
    }

    private static void writeBudgets(BufferedWriter writer, FinancialSummary summary)
            throws IOException {
        writeRow(writer, "category", "limit", "spent", "remaining");
        for (Map.Entry<String, BudgetStatus> entry : summary.budgetStatuses().entrySet()) {
            BudgetStatus status = entry.getValue();
            writeRow(
                    writer,
                    entry.getKey(),
                    money(status.limit()),
                    money(status.spent()),
                    money(status.remaining()));
        }
    }

    private static Map<Long, String> categoryNames(Wallet wallet) {
        Map<Long, String> result = new LinkedHashMap<>();
        result.put(Wallet.UNCATEGORIZED_ID, Wallet.UNCATEGORIZED_NAME);
        for (TransactionType type : TransactionType.values()) {
            result.putAll(wallet.categoryNames(type));
        }
        return result;
    }

    private static String money(BigDecimal value) {
        return value.toPlainString();
    }

    private static void writeRow(BufferedWriter writer, String... values) throws IOException {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escape(values[index]));
        }
        writer.newLine();
    }

    private static String escape(String value) {
        String requiredValue = value == null ? "" : value;
        boolean quote = requiredValue.indexOf(',') >= 0
                || requiredValue.indexOf('"') >= 0
                || requiredValue.indexOf('\n') >= 0
                || requiredValue.indexOf('\r') >= 0;
        if (!quote) {
            return requiredValue;
        }
        return '"' + requiredValue.replace("\"", "\"\"") + '"';
    }
}
