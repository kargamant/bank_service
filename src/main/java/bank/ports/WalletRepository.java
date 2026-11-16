package bank.ports;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import bank.domain.Wallet;

public interface WalletRepository {

    Optional<Wallet> findByOwnerId(UUID ownerId);

    void add(Wallet wallet);

    void restore(Wallet wallet);

    void replace(Wallet wallet);

    void markDirty(UUID ownerId);

    Collection<Wallet> findDirty();

    void markClean(Collection<UUID> ownerIds);

    boolean hasDirty();
}
