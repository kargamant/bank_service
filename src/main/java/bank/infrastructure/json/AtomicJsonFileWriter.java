package bank.infrastructure.json;

import bank.ports.StorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AtomicJsonFileWriter {

    private AtomicJsonFileWriter() {
    }

    static void write(ObjectMapper mapper, Path target, Object value) {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        Path temporary = null;

        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(
                    parent, "tmp-" + absoluteTarget.getFileName() + "-", ".json");
            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporary, StandardCharsets.UTF_8)) {
                mapper.writeValue(writer, value);
            }
            moveReplacing(temporary, absoluteTarget);
            temporary = null;
        } catch (IOException exception) {
            throw new StorageException("cannot write JSON file " + absoluteTarget, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The original write error is more important than temporary-file cleanup.
                }
            }
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
