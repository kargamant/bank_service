package bank.ports;

public interface PasswordHasher {

    byte[] generateSalt();

    String hash(char[] password, byte[] salt);

    boolean matches(char[] password, String expectedHash, byte[] salt);
}
