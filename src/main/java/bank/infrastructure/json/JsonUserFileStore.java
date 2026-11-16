package bank.infrastructure.json;

import bank.domain.User;
import bank.ports.StorageException;
import bank.ports.UserFileStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class JsonUserFileStore implements UserFileStore {

    private final Path usersFile;
    private final ObjectMapper mapper;

    public JsonUserFileStore(Path usersFile, ObjectMapper mapper) {
        this.usersFile = Objects.requireNonNull(usersFile, "usersFile");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public List<User> loadAll() {
        Path absoluteFile = usersFile.toAbsolutePath().normalize();
        if (Files.notExists(absoluteFile)) {
            return List.of();
        }

        try {
            UserRegistryDocument document = mapper.readValue(
                    absoluteFile.toFile(), UserRegistryDocument.class);
            validateDocument(document);
            Set<UUID> userIds = new HashSet<>();
            Set<String> logins = new HashSet<>();
            return document.users().stream().map(userData -> {
                if (userData == null) {
                    throw new IllegalArgumentException("user registry contains null user");
                }
                User user = userData.toDomain();
                if (!userIds.add(user.id())) {
                    throw new IllegalArgumentException(
                            "user registry contains duplicate id " + user.id());
                }
                if (!logins.add(user.login())) {
                    throw new IllegalArgumentException(
                            "user registry contains duplicate login " + user.login());
                }
                return user;
            }).toList();
        } catch (IOException | RuntimeException exception) {
            throw new StorageException("cannot read user registry " + absoluteFile, exception);
        }
    }

    @Override
    public void saveAll(Collection<User> users) {
        Objects.requireNonNull(users, "users");
        List<UserData> userData = users.stream()
                .map(user -> UserData.fromDomain(Objects.requireNonNull(user, "user")))
                .sorted(Comparator.comparing(UserData::login))
                .toList();
        AtomicJsonFileWriter.write(
                mapper,
                usersFile,
                new UserRegistryDocument(userData));
    }

    private static void validateDocument(UserRegistryDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("user registry is empty");
        }
        if (document.users() == null) {
            throw new IllegalArgumentException("user registry contains no user list");
        }
    }
}
