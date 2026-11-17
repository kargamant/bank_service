package bank.infrastructure.json;

import bank.domain.Wallet;
import bank.ports.SnapshotPort;
import bank.ports.StorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class JsonSnapshotAdapter implements SnapshotPort {

    private final ObjectMapper mapper;

    public JsonSnapshotAdapter(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void write(Path destination, Wallet wallet) {
        AtomicJsonFileWriter.write(
                mapper,
                Objects.requireNonNull(destination, "destination"),
                WalletDocument.fromDomain(Objects.requireNonNull(wallet, "wallet")));
    }

    @Override
    public Wallet read(Path source) {
        Path absoluteSource = Objects.requireNonNull(source, "source")
                .toAbsolutePath().normalize();
        if (Files.notExists(absoluteSource)) {
            throw new StorageException("snapshot does not exist: " + absoluteSource);
        }
        try {
            WalletDocument document = mapper.readValue(
                    absoluteSource.toFile(), WalletDocument.class);
            if (document == null) {
                throw new IllegalArgumentException("snapshot is empty");
            }
            return document.toDomain();
        } catch (IOException | RuntimeException exception) {
            throw new StorageException("cannot read snapshot " + absoluteSource, exception);
        }
    }
}
