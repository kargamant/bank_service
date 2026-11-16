package bank.infrastructure.memory;

import bank.domain.User;
import bank.ports.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, User> usersById = new LinkedHashMap<>();
    private final Map<String, UUID> userIdsByLogin = new LinkedHashMap<>();
    private final Set<UUID> dirtyUserIds = new LinkedHashSet<>();

    @Override
    public Optional<User> findByLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        UUID userId = userIdsByLogin.get(login);
        return userId == null ? Optional.empty() : Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Collection<User> findAll() {
        return List.copyOf(usersById.values());
    }

    @Override
    public void add(User user) {
        Objects.requireNonNull(user, "user");
        if (usersById.containsKey(user.id())) {
            throw new IllegalStateException("user already exists: " + user.id());
        }
        ensureLoginAvailable(user.login(), userIdsByLogin);

        usersById.put(user.id(), user);
        userIdsByLogin.put(user.login(), user.id());
        dirtyUserIds.add(user.id());
    }

    @Override
    public void replaceAll(Collection<User> users) {
        Objects.requireNonNull(users, "users");
        Map<UUID, User> replacementUsers = new LinkedHashMap<>();
        Map<String, UUID> replacementLogins = new LinkedHashMap<>();

        for (User user : users) {
            User requiredUser = Objects.requireNonNull(user, "user entry");
            if (replacementUsers.putIfAbsent(requiredUser.id(), requiredUser) != null) {
                throw new IllegalStateException("duplicate user id " + requiredUser.id());
            }
            ensureLoginAvailable(requiredUser.login(), replacementLogins);
            replacementLogins.put(requiredUser.login(), requiredUser.id());
        }

        usersById.clear();
        usersById.putAll(replacementUsers);
        userIdsByLogin.clear();
        userIdsByLogin.putAll(replacementLogins);
        dirtyUserIds.clear();
    }

    @Override
    public Collection<User> findDirty() {
        List<User> result = new ArrayList<>();
        for (UUID userId : dirtyUserIds) {
            User user = usersById.get(userId);
            if (user != null) {
                result.add(user);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public void markClean(Collection<UUID> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        dirtyUserIds.removeAll(userIds);
    }

    @Override
    public boolean hasDirty() {
        return !dirtyUserIds.isEmpty();
    }

    private static void ensureLoginAvailable(String login, Map<String, UUID> loginIndex) {
        if (loginIndex.containsKey(login)) {
            throw new IllegalStateException("login already exists: " + login);
        }
    }
}
