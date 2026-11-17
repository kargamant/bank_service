package bank.cli;

import java.util.Locale;
import java.util.Map;

final class CommandHelp {

    private static final String GENERAL = """
            Available commands:
              help [command]
              register <login> <password>
              login <login> <password>
              logout | save | exit
              category add|list|rename|delete ...
              income add <amount> <category> [description]
              expense add <amount> <category> [description]
              transactions [category]
              budget set|edit|delete|list ...
              summary [categories <category> ...]
              transfer <recipient-login> <amount> [description]
              export csv|json <path>
              import json <path>

            Example: expense add 300.0 "food" "burger king"
            """;

    private static final Map<String, String> TOPICS = Map.ofEntries(
            Map.entry("register", "register <login> <password>"),
            Map.entry("login", "login <login> <password>"),
            Map.entry("category", """
                    category add <income|expense> <name>
                    category list [income|expense]
                    category rename <current-name> <new-name>
                    category delete <name>
                    """),
            Map.entry("income", "income add <amount> <category> [description]"),
            Map.entry("expense", "expense add <amount> <category> [description]"),
            Map.entry("transactions", "transactions [category]"),
            Map.entry("budget", """
                    budget set <category> <amount>
                    budget edit <category> <amount>
                    budget delete <category>
                    budget list
                    """),
            Map.entry("summary", """
                    summary
                    summary categories <category> [category ...]
                    """),
            Map.entry("transfer", "transfer <recipient-login> <amount> [description]"),
            Map.entry("export", """
                    export csv <path>
                    export json <path>
                    """),
            Map.entry("import", "import json <path>"),
            Map.entry("save", "save"),
            Map.entry("logout", "logout"),
            Map.entry("exit", "exit"));

    private CommandHelp() {
    }

    static String general() {
        return GENERAL;
    }

    static String topic(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        String help = TOPICS.get(normalized);
        if (help == null) {
            throw new CommandSyntaxException("no help topic for command: " + command);
        }
        return help;
    }
}
