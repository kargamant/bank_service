package bank.infrastructure.memory;

import bank.domain.Wallet;
import bank.ports.WalletRepository;
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

public final class InMemoryWalletRepository implements WalletRepository {

    private final Map<UUID, Wallet> walletsByOwnerId = new LinkedHashMap<>();
    private final Set<UUID> dirtyOwnerIds = new LinkedHashSet<>();

    @Override
    public Optional<Wallet> findByOwnerId(UUID ownerId) {
        return Optional.ofNullable(walletsByOwnerId.get(ownerId));
    }

    @Override
    public void add(Wallet wallet) {
        Objects.requireNonNull(wallet, "wallet");
        if (walletsByOwnerId.containsKey(wallet.ownerId())) {
            throw new IllegalStateException(
                    "wallet already exists for owner " + wallet.ownerId());
        }
        walletsByOwnerId.put(wallet.ownerId(), wallet);
        dirtyOwnerIds.add(wallet.ownerId());
    }

    @Override
    public void restore(Wallet wallet) {
        Objects.requireNonNull(wallet, "wallet");
        if (walletsByOwnerId.containsKey(wallet.ownerId())) {
            throw new IllegalStateException(
                    "wallet is already loaded for owner " + wallet.ownerId());
        }
        walletsByOwnerId.put(wallet.ownerId(), wallet);
        dirtyOwnerIds.remove(wallet.ownerId());
    }

    @Override
    public void markDirty(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        if (!walletsByOwnerId.containsKey(ownerId)) {
            throw new IllegalStateException("wallet is not loaded for owner " + ownerId);
        }
        dirtyOwnerIds.add(ownerId);
    }

    @Override
    public Collection<Wallet> findDirty() {
        List<Wallet> result = new ArrayList<>();
        for (UUID ownerId : dirtyOwnerIds) {
            Wallet wallet = walletsByOwnerId.get(ownerId);
            if (wallet != null) {
                result.add(wallet);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public void markClean(Collection<UUID> ownerIds) {
        Objects.requireNonNull(ownerIds, "ownerIds");
        dirtyOwnerIds.removeAll(ownerIds);
    }

    @Override
    public boolean hasDirty() {
        return !dirtyOwnerIds.isEmpty();
    }
}
