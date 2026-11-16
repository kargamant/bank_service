package bank.application;

import bank.domain.User;
import bank.domain.Wallet;
import bank.ports.UserFileStore;
import bank.ports.UserRepository;
import bank.ports.WalletFileStore;
import bank.ports.WalletRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PersistenceService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserFileStore userFileStore;
    private final WalletFileStore walletFileStore;

    public PersistenceService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            UserFileStore userFileStore,
            WalletFileStore walletFileStore) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
        this.userFileStore = Objects.requireNonNull(userFileStore, "userFileStore");
        this.walletFileStore = Objects.requireNonNull(walletFileStore, "walletFileStore");
    }

    public int loadUsers() {
        List<User> users = userFileStore.loadAll();
        userRepository.replaceAll(users);
        return users.size();
    }

    public Wallet loadWallet(UUID ownerId) {
        UUID requiredOwnerId = Objects.requireNonNull(ownerId, "ownerId");
        Optional<Wallet> loadedWallet = walletRepository.findByOwnerId(requiredOwnerId);
        if (loadedWallet.isPresent()) {
            return loadedWallet.get();
        }

        Optional<Wallet> storedWallet = walletFileStore.load(requiredOwnerId);
        if (storedWallet.isPresent()) {
            Wallet wallet = storedWallet.get();
            walletRepository.restore(wallet);
            return wallet;
        }

        Wallet newWallet = new Wallet(requiredOwnerId);
        walletRepository.add(newWallet);
        return newWallet;
    }

    public void saveUsers() {
        Collection<User> dirtyUsers = userRepository.findDirty();
        if (dirtyUsers.isEmpty()) {
            return;
        }

        userFileStore.saveAll(userRepository.findAll());
        userRepository.markClean(dirtyUsers.stream().map(User::id).toList());
    }

    public void saveWallet(UUID ownerId) {
        UUID requiredOwnerId = Objects.requireNonNull(ownerId, "ownerId");
        Wallet wallet = walletRepository.findByOwnerId(requiredOwnerId)
                .orElseThrow(() -> new IllegalStateException(
                        "wallet for owner " + requiredOwnerId + " is not loaded"));
        walletFileStore.save(wallet);
        walletRepository.markClean(List.of(requiredOwnerId));
    }

    public void saveWallets(Collection<UUID> ownerIds) {
        Objects.requireNonNull(ownerIds, "ownerIds");
        Set<UUID> uniqueOwnerIds = new LinkedHashSet<>(ownerIds);
        if (uniqueOwnerIds.isEmpty()) {
            return;
        }

        List<Wallet> wallets = uniqueOwnerIds.stream()
                .map(ownerId -> walletRepository.findByOwnerId(ownerId)
                        .orElseThrow(() -> new IllegalStateException(
                                "wallet for owner " + ownerId + " is not loaded")))
                .toList();

        for (Wallet wallet : wallets) {
            walletFileStore.save(wallet);
        }
        walletRepository.markClean(uniqueOwnerIds);
    }

    public void saveDirtyState() {
        saveUsers();
        Collection<Wallet> dirtyWallets = walletRepository.findDirty();
        saveWallets(dirtyWallets.stream().map(Wallet::ownerId).toList());
    }

    public boolean hasUnsavedChanges() {
        return userRepository.hasDirty() || walletRepository.hasDirty();
    }
}
