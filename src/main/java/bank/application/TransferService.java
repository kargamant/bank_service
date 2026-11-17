package bank.application;

import bank.application.exception.ApplicationException;
import bank.domain.Transaction;
import bank.domain.TransactionType;
import bank.domain.User;
import bank.domain.Wallet;
import bank.ports.UserRepository;
import bank.ports.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TransferService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PersistenceService persistenceService;

    public TransferService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PersistenceService persistenceService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
        this.persistenceService = Objects.requireNonNull(
                persistenceService, "persistenceService");
    }

    public TransferResult transfer(
            UUID senderId,
            String recipientLogin,
            BigDecimal amount,
            String description) {
        User sender = userRepository.findById(Objects.requireNonNull(senderId, "senderId"))
                .orElseThrow(() -> new ApplicationException("sender not found"));
        if (recipientLogin == null || recipientLogin.isBlank()) {
            throw new ApplicationException("recipient login cannot be empty");
        }
        User recipient = userRepository.findByLogin(recipientLogin.trim())
                .orElseThrow(() -> new ApplicationException(
                        "recipient not found: " + recipientLogin.trim()));
        if (sender.id().equals(recipient.id())) {
            throw new ApplicationException("cannot transfer money to the same wallet");
        }

        Wallet senderWallet = persistenceService.loadWallet(sender.id());
        Wallet recipientWallet = persistenceService.loadWallet(recipient.id());

        UUID transferId = UUID.randomUUID();
        Transaction expense = Transaction.createTransfer(
                TransactionType.EXPENSE, amount, description, transferId);
        Transaction income = Transaction.createTransfer(
                TransactionType.INCOME, amount, description, transferId);

        senderWallet.addTransaction(Wallet.UNCATEGORIZED_ID, expense);
        recipientWallet.addTransaction(Wallet.UNCATEGORIZED_ID, income);
        walletRepository.markDirty(sender.id());
        walletRepository.markDirty(recipient.id());

        List<FinancialWarning> warnings = new ArrayList<>();
        FinanceService.addExpenseIncomeWarning(senderWallet, warnings);
        return new TransferResult(
                transferId,
                sender.login(),
                recipient.login(),
                amount,
                senderWallet.balance(),
                recipientWallet.balance(),
                warnings);
    }
}
