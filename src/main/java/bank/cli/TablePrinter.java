package bank.cli;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TablePrinter {

    private final PrintStream out;

    public TablePrinter(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    public void print(List<String> headers, List<List<String>> rows) {
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(rows, "rows");
        if (headers.isEmpty()) {
            return;
        }
        if (rows.isEmpty()) {
            out.println("(no data)");
            return;
        }

        List<Integer> widths = new ArrayList<>();
        for (String header : headers) {
            widths.add(header.length());
        }
        for (List<String> row : rows) {
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException("table row has an unexpected column count");
            }
            for (int index = 0; index < row.size(); index++) {
                widths.set(index, Math.max(widths.get(index), safe(row.get(index)).length()));
            }
        }

        printRow(headers, widths);
        printSeparator(widths);
        for (List<String> row : rows) {
            printRow(row, widths);
        }
    }

    public static String money(BigDecimal value) {
        return Objects.requireNonNull(value, "value")
                .setScale(1, RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    private void printRow(List<String> values, List<Integer> widths) {
        StringBuilder line = new StringBuilder("| ");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                line.append(" | ");
            }
            line.append(String.format("%-" + widths.get(index) + "s", safe(values.get(index))));
        }
        line.append(" |");
        out.println(line);
    }

    private void printSeparator(List<Integer> widths) {
        StringBuilder line = new StringBuilder("+-");
        for (int index = 0; index < widths.size(); index++) {
            if (index > 0) {
                line.append("-+-");
            }
            line.append("-".repeat(widths.get(index)));
        }
        line.append("-+");
        out.println(line);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
