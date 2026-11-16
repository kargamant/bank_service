package bank.domain.exception;

public final class CategoryNotFoundException extends DomainException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
