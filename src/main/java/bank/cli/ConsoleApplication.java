package bank.cli;

import bank.application.PersistenceService;
import bank.application.Session;
import bank.ports.StorageException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;

public final class ConsoleApplication {

    private final BufferedReader input;
    private final PrintStream out;
    private final CommandParser commandParser;
    private final CommandHandler commandHandler;
    private final PersistenceService persistenceService;
    private final Session session;

    public ConsoleApplication(
            BufferedReader input,
            PrintStream out,
            CommandParser commandParser,
            CommandHandler commandHandler,
            PersistenceService persistenceService,
            Session session) {
        this.input = Objects.requireNonNull(input, "input");
        this.out = Objects.requireNonNull(out, "out");
        this.commandParser = Objects.requireNonNull(commandParser, "commandParser");
        this.commandHandler = Objects.requireNonNull(commandHandler, "commandHandler");
        this.persistenceService = Objects.requireNonNull(
                persistenceService, "persistenceService");
        this.session = Objects.requireNonNull(session, "session");
    }

    public void run() {
        if (!loadUsers()) {
            return;
        }

        out.println("Personal Finance Manager");
        out.println("Type help to see available commands.");
        while (true) {
            printPrompt();
            String line;
            try {
                line = input.readLine();
            } catch (IOException exception) {
                out.println("Input error: " + exception.getMessage());
                saveBeforeUnexpectedEnd();
                return;
            }

            if (line == null) {
                saveBeforeUnexpectedEnd();
                out.println("Goodbye.");
                return;
            }

            try {
                var command = commandParser.parse(line);
                if (command.isPresent() && !commandHandler.handle(command.get())) {
                    return;
                }
            } catch (CommandSyntaxException exception) {
                out.println("Error: " + exception.getMessage());
            }
        }
    }

    private boolean loadUsers() {
        try {
            int users = persistenceService.loadUsers();
            out.println("Loaded users: " + users);
            return true;
        } catch (StorageException | IllegalStateException exception) {
            out.println("Cannot start application: " + exception.getMessage());
            return false;
        }
    }

    private void printPrompt() {
        out.print(session.isAuthenticated() ? "finance> " : "finance[guest]> ");
        out.flush();
    }

    private void saveBeforeUnexpectedEnd() {
        try {
            persistenceService.saveDirtyState();
        } catch (RuntimeException exception) {
            out.println("Warning: unsaved data remains in memory: " + exception.getMessage());
        }
    }
}
