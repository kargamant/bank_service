package bank.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;
import bank.domain.exception.DomainException;

final class DomainValidation {

    private static final int MAX_MONEY_SCALE = 1;
    private static final Pattern LETTERS_AND_SPACES = Pattern.compile("[\\p{L} ]+");

    private DomainValidation() {
    }

    static <T> T requireNotNull(T value, String message) {
        if (value == null) {
            throw new DomainException(message);
        }
        return value;
    }

    static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(message);
        }
        return value.trim();
    }

    static BigDecimal requirePositiveMoney(BigDecimal value, String fieldName) {
        BigDecimal requiredValue = requireMoney(value, fieldName);
        if (requiredValue.signum() <= 0) {
            throw new DomainException(fieldName + " must be greater than zero");
        }
        return requiredValue;
    }

    static BigDecimal requireNonNegativeMoney(BigDecimal value, String fieldName) {
        BigDecimal requiredValue = requireMoney(value, fieldName);
        if (requiredValue.signum() < 0) {
            throw new DomainException(fieldName + " cannot be negative");
        }
        return requiredValue;
    }

    static String normalizeCategoryName(String value) {
        String trimmed = requireNonBlank(value, "category name cannot be empty");
        String normalized = trimmed.replaceAll(" +", " ");
        return requireLettersAndSpaces(normalized, "category name");
    }

    static String requireLettersAndSpaces(String value, String fieldName) {
        if (!LETTERS_AND_SPACES.matcher(value).matches()) {
            throw new DomainException(
                    fieldName + " may contain only letters and spaces");
        }
        return value;
    }

    static String categoryNameKey(String value) {
        return normalizeCategoryName(value).toLowerCase(Locale.ROOT);
    }

    private static BigDecimal requireMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new DomainException(fieldName + " is not specified");
        }
        if (value.stripTrailingZeros().scale() > MAX_MONEY_SCALE) {
            throw new DomainException(
                    fieldName + " must have no more than one decimal place");
        }
        return value;
    }
}
