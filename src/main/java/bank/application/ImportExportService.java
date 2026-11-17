package bank.application;

import bank.application.exception.ApplicationException;
import bank.domain.Wallet;
import bank.ports.ReportExporter;
import bank.ports.SnapshotPort;
import bank.ports.WalletRepository;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public final class ImportExportService {

    private final WalletRepository walletRepository;
    private final StatisticsService statisticsService;
    private final ReportExporter reportExporter;
    private final SnapshotPort snapshotPort;

    public ImportExportService(
            WalletRepository walletRepository,
            StatisticsService statisticsService,
            ReportExporter reportExporter,
            SnapshotPort snapshotPort) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
        this.statisticsService = Objects.requireNonNull(
                statisticsService, "statisticsService");
        this.reportExporter = Objects.requireNonNull(reportExporter, "reportExporter");
        this.snapshotPort = Objects.requireNonNull(snapshotPort, "snapshotPort");
    }

    public void exportCsv(UUID ownerId, Path destination) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        FinancialSummary summary = statisticsService.summarize(ownerId);
        reportExporter.export(
                Objects.requireNonNull(destination, "destination"), wallet, summary);
    }

    public void exportJson(UUID ownerId, Path destination) {
        Wallet wallet = ServiceSupport.requireWallet(walletRepository, ownerId);
        snapshotPort.write(Objects.requireNonNull(destination, "destination"), wallet);
    }

    public Wallet importJson(UUID ownerId, Path source) {
        UUID requiredOwnerId = Objects.requireNonNull(ownerId, "ownerId");
        ServiceSupport.requireWallet(walletRepository, requiredOwnerId);
        Wallet snapshot = snapshotPort.read(Objects.requireNonNull(source, "source"));
        if (!requiredOwnerId.equals(snapshot.ownerId())) {
            throw new ApplicationException("snapshot belongs to another user");
        }
        walletRepository.replace(snapshot);
        return snapshot;
    }
}
