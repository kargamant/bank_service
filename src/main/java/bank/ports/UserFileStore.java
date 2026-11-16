package bank.ports;

import java.util.Collection;
import java.util.List;
import bank.domain.User;

public interface UserFileStore {

    List<User> loadAll();

    void saveAll(Collection<User> users);
}
