package bank.domain;

import java.math.BigDecimal;
import java.util.Locale;
import bank.domain.exception.DomainValidationException;

final class DomainValidation {

    private static final int MAX_MONEY_SCALE = 1;

    private DomainValidation() {
    }

    static <T> T requireNotNull(T value, String message) {
        if (value == null) {
            throw new DomainValidationException(message);
        }
        return value;
    }

    static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(message);
        }
        return value.trim();
    }

    static BigDecimal requirePositiveMoney(BigDecimal value, String fieldName) {
        BigDecimal requiredValue = requireMoney(value, fieldName);
        if (requiredValue.signum() <= 0) {
            throw new DomainValidationException(fieldName + " must be greater than zero");
        }
        return requiredValue;
    }

    static BigDecimal requireNonNegativeMoney(BigDecimal value, String fieldName) {
        BigDecimal requiredValue = requireMoney(value, fieldName);
        if (requiredValue.signum() < 0) {
            throw new DomainValidationException(fieldName + " cannot be negative");
        }
        return requiredValue;
    }

    static String normalizeCategoryName(String value) {
        String trimmed = requireNonBlank(value, "category name cannot be empty");
        return trimmed.replaceAll("\\s+", " ");
    }

    static String categoryNameKey(String value) {
        return normalizeCategoryName(value).toLowerCase(Locale.ROOT);
    }

    private static BigDecimal requireMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new DomainValidationException(fieldName + " is not specified");
        }
        if (value.stripTrailingZeros().scale() > MAX_MONEY_SCALE) {
            throw new DomainValidationException(
                    fieldName + " must have no more than one decimal place");
        }
        return value;
    }
}
