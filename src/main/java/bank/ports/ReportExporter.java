package bank.ports;

import java.nio.file.Path;
import bank.application.FinancialSummary;
import bank.domain.Wallet;

public interface ReportExporter {

    void export(Path destination, Wallet wallet, FinancialSummary summary);
}
