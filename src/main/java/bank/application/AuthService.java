package bank.application;

import bank.application.exception.ApplicationException;
import bank.domain.User;
import bank.domain.Wallet;
import bank.ports.PasswordHasher;
import bank.ports.UserRepository;
import bank.ports.WalletRepository;
import java.util.Base64;
import java.util.Objects;

public final class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordHasher passwordHasher;
    private final PersistenceService persistenceService;
    private final Session session;

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordHasher passwordHasher,
            PersistenceService persistenceService,
            Session session) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.persistenceService = Objects.requireNonNull(
                persistenceService, "persistenceService");
        this.session = Objects.requireNonNull(session, "session");
    }

    public User register(String login, char[] password) {
        String normalizedLogin = requireLogin(login);
        requirePassword(password);
        if (userRepository.findByLogin(normalizedLogin).isPresent()) {
            throw new ApplicationException("login already exists: " + normalizedLogin);
        }

        byte[] salt = passwordHasher.generateSalt();
        String passwordHash = passwordHasher.hash(password, salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        User user = User.create(normalizedLogin, passwordHash, encodedSalt);

        userRepository.add(user);
        walletRepository.add(new Wallet(user.id()));
        return user;
    }

    public Session login(String login, char[] password) {
        if (session.isAuthenticated()) {
            throw new ApplicationException("logout before signing in as another user");
        }
        String normalizedLogin = requireLogin(login);
        requirePassword(password);
        User user = userRepository.findByLogin(normalizedLogin)
                .orElseThrow(AuthService::invalidCredentials);

        byte[] salt;
        try {
            salt = Base64.getDecoder().decode(user.passwordSalt());
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException("stored password salt is invalid", exception);
        }

        if (!passwordHasher.matches(password, user.passwordHash(), salt)) {
            throw invalidCredentials();
        }

        persistenceService.loadWallet(user.id());
        session.start(user.id());
        return session;
    }

    public void logout() {
        session.requireUserId();
        persistenceService.saveDirtyState();
        session.close();
    }

    private static String requireLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new ApplicationException("login cannot be empty");
        }
        return login.trim();
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new ApplicationException("password cannot be empty");
        }
    }

    private static ApplicationException invalidCredentials() {
        return new ApplicationException("invalid login or password");
    }
}
