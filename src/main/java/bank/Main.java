package bank;

import bank.application.AuthService;
import bank.application.BudgetService;
import bank.application.CategoryService;
import bank.application.FinanceService;
import bank.application.ImportExportService;
import bank.application.PersistenceService;
import bank.application.Session;
import bank.application.StatisticsService;
import bank.application.TransferService;
import bank.cli.CommandHandler;
import bank.cli.CommandParser;
import bank.cli.ConsoleApplication;
import bank.cli.TablePrinter;
import bank.infrastructure.export.CsvReportExporter;
import bank.infrastructure.json.JsonMapperFactory;
import bank.infrastructure.json.JsonSnapshotAdapter;
import bank.infrastructure.json.JsonUserFileStore;
import bank.infrastructure.json.JsonWalletFileStore;
import bank.infrastructure.memory.InMemoryUserRepository;
import bank.infrastructure.memory.InMemoryWalletRepository;
import bank.infrastructure.security.Pbkdf2PasswordHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path dataDirectory = Path.of("data");
        ObjectMapper mapper = JsonMapperFactory.create();

        var userRepository = new InMemoryUserRepository();
        var walletRepository = new InMemoryWalletRepository();
        var userFileStore = new JsonUserFileStore(
                dataDirectory.resolve("users.json"), mapper);
        var walletFileStore = new JsonWalletFileStore(
                dataDirectory.resolve("wallets"), mapper);
        var persistenceService = new PersistenceService(
                userRepository, walletRepository, userFileStore, walletFileStore);

        var session = new Session();
        var authService = new AuthService(
                userRepository,
                walletRepository,
                new Pbkdf2PasswordHasher(),
                persistenceService,
                session);
        var categoryService = new CategoryService(walletRepository);
        var financeService = new FinanceService(walletRepository);
        var budgetService = new BudgetService(walletRepository);
        var statisticsService = new StatisticsService(walletRepository);
        var transferService = new TransferService(
                userRepository, walletRepository, persistenceService);
        var importExportService = new ImportExportService(
                walletRepository,
                statisticsService,
                new CsvReportExporter(),
                new JsonSnapshotAdapter(mapper));

        var out = System.out;
        var tablePrinter = new TablePrinter(out);
        var commandHandler = new CommandHandler(
                authService,
                categoryService,
                financeService,
                budgetService,
                statisticsService,
                transferService,
                importExportService,
                persistenceService,
                session,
                tablePrinter,
                out);
        var application = new ConsoleApplication(
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                out,
                new CommandParser(),
                commandHandler,
                persistenceService,
                session);
        application.run();
    }
}
