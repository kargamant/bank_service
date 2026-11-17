package bank.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CommandParser {

    public Optional<ParsedCommand> parse(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean tokenStarted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
                tokenStarted = true;
                continue;
            }
            if (Character.isWhitespace(character) && !quoted) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }
            current.append(character);
            tokenStarted = true;
        }

        if (quoted) {
            throw new CommandSyntaxException("unclosed double quote");
        }
        if (tokenStarted) {
            tokens.add(current.toString());
        }
        return tokens.isEmpty()
                ? Optional.empty()
                : Optional.of(new ParsedCommand(tokens));
    }
}
