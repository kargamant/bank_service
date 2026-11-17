package bank.cli;

public final class CommandSyntaxException extends RuntimeException {

    public CommandSyntaxException(String message) {
        super(message);
    }
}
