package bank.infrastructure.security;

import bank.ports.PasswordHasher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;

    private final SecureRandom secureRandom;

    public Pbkdf2PasswordHasher() {
        this(new SecureRandom());
    }

    Pbkdf2PasswordHasher(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    @Override
    public String hash(char[] password, byte[] salt) {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(salt, "salt");
        PBEKeySpec specification = new PBEKeySpec(
                password, salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return Base64.getEncoder().encodeToString(
                    factory.generateSecret(specification).getEncoded());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("PBKDF2 is not available", exception);
        } catch (java.security.spec.InvalidKeySpecException exception) {
            throw new IllegalStateException("cannot hash password", exception);
        } finally {
            specification.clearPassword();
        }
    }

    @Override
    public boolean matches(char[] password, String expectedHash, byte[] salt) {
        Objects.requireNonNull(expectedHash, "expectedHash");
        byte[] expected;
        try {
            expected = Base64.getDecoder().decode(expectedHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        byte[] actual = Base64.getDecoder().decode(hash(password, salt));
        return MessageDigest.isEqual(expected, actual);
    }
}
