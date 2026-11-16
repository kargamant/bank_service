package bank.ports;

import java.util.Optional;
import java.util.UUID;
import bank.domain.Wallet;

public interface WalletFileStore {

    Optional<Wallet> load(UUID ownerId);

    void save(Wallet wallet);
}
