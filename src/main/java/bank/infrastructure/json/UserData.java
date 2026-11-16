package bank.infrastructure.json;

import bank.domain.User;
import java.util.UUID;

record UserData(UUID id, String login, String passwordHash, String passwordSalt) {

    static UserData fromDomain(User user) {
        return new UserData(
                user.id(), user.login(), user.passwordHash(), user.passwordSalt());
    }

    User toDomain() {
        return new User(id, login, passwordHash, passwordSalt);
    }
}
