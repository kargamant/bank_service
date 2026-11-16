package bank.domain.exception;

public final class BudgetNotFoundException extends DomainException {

    public BudgetNotFoundException(String message) {
        super(message);
    }
}
