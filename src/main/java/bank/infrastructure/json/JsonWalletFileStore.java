package bank.infrastructure.json;

import bank.domain.Wallet;
import bank.ports.StorageException;
import bank.ports.WalletFileStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JsonWalletFileStore implements WalletFileStore {

    private final Path walletsDirectory;
    private final ObjectMapper mapper;

    public JsonWalletFileStore(Path walletsDirectory, ObjectMapper mapper) {
        this.walletsDirectory = Objects.requireNonNull(
                walletsDirectory, "walletsDirectory");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Optional<Wallet> load(UUID ownerId) {
        UUID requiredOwnerId = Objects.requireNonNull(ownerId, "ownerId");
        Path walletFile = walletFile(requiredOwnerId);
        if (Files.notExists(walletFile)) {
            return Optional.empty();
        }

        try {
            WalletDocument document = mapper.readValue(
                    walletFile.toFile(), WalletDocument.class);
            validateDocument(document, requiredOwnerId);
            return Optional.of(document.toDomain());
        } catch (IOException | RuntimeException exception) {
            throw new StorageException("cannot read wallet " + walletFile, exception);
        }
    }

    @Override
    public void save(Wallet wallet) {
        Wallet requiredWallet = Objects.requireNonNull(wallet, "wallet");
        AtomicJsonFileWriter.write(
                mapper,
                walletFile(requiredWallet.ownerId()),
                WalletDocument.fromDomain(requiredWallet));
    }

    private Path walletFile(UUID ownerId) {
        return walletsDirectory.toAbsolutePath().normalize().resolve(ownerId + ".json");
    }

    private static void validateDocument(WalletDocument document, UUID expectedOwnerId) {
        if (document == null) {
            throw new IllegalArgumentException("wallet file is empty");
        }
        if (!expectedOwnerId.equals(document.ownerId())) {
            throw new IllegalArgumentException(
                    "wallet owner does not match file name: " + expectedOwnerId);
        }
    }
}
