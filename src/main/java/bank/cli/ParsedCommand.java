package bank.cli;

import java.util.List;
import java.util.Objects;

public record ParsedCommand(List<String> tokens) {

    public ParsedCommand {
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("command cannot be empty");
        }
    }

    public String name() {
        return tokens.getFirst().toLowerCase(java.util.Locale.ROOT);
    }
}
