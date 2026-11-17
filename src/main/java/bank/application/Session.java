package bank.application;

import bank.application.exception.ApplicationException;
import java.util.Optional;
import java.util.UUID;

public final class Session {

    private UUID currentUserId;

    public boolean isAuthenticated() {
        return currentUserId != null;
    }

    public Optional<UUID> currentUserId() {
        return Optional.ofNullable(currentUserId);
    }

    public UUID requireUserId() {
        if (currentUserId == null) {
            throw new ApplicationException("authentication is required");
        }
        return currentUserId;
    }

    void start(UUID userId) {
        if (isAuthenticated()) {
            throw new ApplicationException("another user is already authenticated");
        }
        currentUserId = java.util.Objects.requireNonNull(userId, "userId");
    }

    void close() {
        currentUserId = null;
    }
}
