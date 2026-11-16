package bank.ports;

import java.nio.file.Path;
import bank.domain.Wallet;

public interface SnapshotPort {

    void write(Path destination, Wallet wallet);

    Wallet read(Path source);
}
