package bank.ports;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import bank.domain.User;

public interface UserRepository {

    Optional<User> findByLogin(String login);

    Optional<User> findById(UUID id);

    Collection<User> findAll();

    void add(User user);

    void replaceAll(Collection<User> users);

    Collection<User> findDirty();

    void markClean(Collection<UUID> userIds);

    boolean hasDirty();
}
