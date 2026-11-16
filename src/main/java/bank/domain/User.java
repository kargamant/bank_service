package bank.domain;

import java.util.Objects;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String login;
    private final String passwordHash;
    private final String passwordSalt;

    public User(UUID id, String login, String passwordHash, String passwordSalt) {
        this.id = DomainValidation.requireNotNull(id, "no id specified ");
        this.login = DomainValidation.requireNonBlank(login, "login cant be empty");
        this.passwordHash = DomainValidation.requireNonBlank(
                passwordHash, "pswd hash cant be empty");
        this.passwordSalt = DomainValidation.requireNonBlank(
                passwordSalt, "pswd salt cant be empty");
    }

    public static User create(String login, String passwordHash, String passwordSalt) {
        return new User(UUID.randomUUID(), login, passwordHash, passwordSalt);
    }

    public UUID id() {
        return id;
    }

    public String login() {
        return login;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String passwordSalt() {
        return passwordSalt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
