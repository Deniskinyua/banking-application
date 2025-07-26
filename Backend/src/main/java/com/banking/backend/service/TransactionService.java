package com.banking.backend.service;

import com.banking.backend.GlobalExceptationHandlers.InsufficientBalanceException;
import com.banking.backend.GlobalExceptationHandlers.LimitExceededException;
import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.enums.TransactionType;
import com.banking.backend.model.Account;
import com.banking.backend.model.Transaction;
import com.banking.backend.repository.AccountRepository;
import com.banking.backend.service.interfaces.ITransactionService;
import com.banking.backend.util.TransactionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service class responsible for handling financial transaction business logic
 * particularly fund transfers between user accounts. It encapsulates the core business logic
 * including validation, debit/credit operations and integration with notification services
 *
 * @author Denis Kinyua
 * @version 1.0
 * @since 10/07/2025
 */
@Service
public class TransactionService implements ITransactionService {

    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(AccountRepository accountRepository, NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
    }

    /**
     * Initiates and processes a fund transfer between two customer accounts.
     * This method handles the entire fund transfer lifecycle including:
     * <ul>
     * <li>Retrieving sender and recipient accounts.</li>
     * <li>Performing essential transaction validations (e.g., sufficient balance, daily limit).</li>
     * <li>Debiting the sender's account and crediting the recipient's account.</li>
     * <li>Recording corresponding debit and credit transactions.</li>
     * <li>Persisting updated account and transaction data.</li>
     * <li>Asynchronously sending transfer notifications.</li>
     * </ul>
     * The operation is atomic, ensuring both debit and credit succeed or both fail.
     *
     * @param request The {@link TransactionRequestDTO} containing the details for the fund transfer,
     * including sender and recipient customer IDs and the transfer amount.
     * @throws IllegalArgumentException If the sender or recipient account is not found, or if
     * the transfer is attempted to the same account.
     * @throws InsufficientBalanceException If the sender's account has insufficient funds.
     * @throws LimitExceededException If the sender's daily transaction limit would be exceeded.
     */
    @Transactional
    public void transferFunds(TransactionRequestDTO request) {
        log.info("Attempting fund transfer from user {} to user {} for amount {}",
                request.getFromUserId(), request.getToUserId(), request.getAmount());

        Account fromAccount = accountRepository.findByCustomerId(request.getFromUserId())
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found for user ID: " + request.getFromUserId()));
        Account toAccount = accountRepository.findByCustomerId(request.getToUserId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient account not found for user ID: " + request.getToUserId()));

        // Self-transfer check
        if (fromAccount.getCustomerId().equals(toAccount.getCustomerId())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same account.");
        }

        try {
            validateTransaction(fromAccount, request.getAmount());
        } catch (InsufficientBalanceException | LimitExceededException e) {
            log.warn("Transaction validation failed for user {}: {}", fromAccount.getCustomerId(), e.getMessage());
            // Re-throw specific business exceptions that GlobalExceptionHandler can map to HTTP status
            throw e;
        }

        String transactionId = TransactionIdGenerator.generate();
        log.debug("Generated transaction ID: {}", transactionId);

        // Debit sender account
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        fromAccount.setDailyTransactionAmount(
                fromAccount.getDailyTransactionAmount().add(request.getAmount())
        );
        fromAccount.setDailyTransactionLimit(fromAccount.getDailyTransactionLimit().subtract(request.getAmount()));

        Transaction debitTransaction = createTransaction(transactionId,
                TransactionType.TRANSFER_OUT, request.getAmount().negate(),
                String.format("Transfer to %s (%s)", toAccount.getCustomerName(), toAccount.getCustomerId()), // More descriptive
                LocalDateTime.now(), fromAccount.getBalance());

        fromAccount.addTransaction(debitTransaction);
        accountRepository.save(fromAccount); // Save sender's account with transaction

        // Credit recipient account
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        Transaction creditTransaction = createTransaction(
                transactionId, TransactionType.TRANSFER_IN, request.getAmount(),
                String.format("Transfer from %s (%s)", fromAccount.getCustomerName(), fromAccount.getCustomerId()), // More descriptive
                LocalDateTime.now(),
                toAccount.getBalance()
        );
        toAccount.addTransaction(creditTransaction);
        accountRepository.save(toAccount); // Save recipient's account with transaction

        log.info("Funds transferred successfully for transaction ID: {}", transactionId);

        // ASYNCHRONOUS NOTIFICATION: Send message to Azure Service Bus Queue
        // This call will now enqueue the notification, and the actual sending
        // (e.g., email/SMS) will happen by the listener independently.
        // It's crucial this part doesn't block or rollback the primary financial transaction.
        // If sending to the queue fails, it's a notification system issue, not a financial one.
        try {
            notificationService.sendTransferNotifications(transactionId, fromAccount, toAccount, request.getAmount());
            log.debug("Notification enqueued for transaction ID: {}", transactionId);
        } catch (Exception e) {
            // Log the failure to send notification to queue, but do NOT roll back the financial transaction
            // The financial transaction (debit/credit) should already be committed at this point
            // due to @Transactional and successful saves.
            log.error("Failed to enqueue notification for transaction ID: {}. This will NOT rollback the financial transaction.", transactionId, e);
            // Consider alternative notification methods or a monitoring alert here.
        }
    }

    /**
     * Helper method to create a new Transaction entity.
     *
     * @param id The unique identifier for the transaction.
     * @param type The type of transaction (e.g., TRANSFER_IN, TRANSFER_OUT).
     * @param amount The amount of the transaction.
     * @param description A descriptive string for the transaction.
     * @param timestamp The exact time the transaction occurred.
     * @param balanceAfter The account balance after this transaction.
     * @return A new {@link Transaction} entity.
     */
    private Transaction createTransaction(String id, TransactionType type, BigDecimal amount,
                                          String description, LocalDateTime timestamp, BigDecimal balanceAfter){
        Transaction transaction = new Transaction();
        transaction.setTransactionId(id);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTimestamp(timestamp);
        transaction.setBalanceAfter(balanceAfter);
        return transaction;
    }

    /**
     * Validates if an account has sufficient balance and has not exceeded its daily transaction limit
     * for a given amount.
     *
     * @param account The {@link Account} object to validate.
     * @param amount The {@link BigDecimal} amount for the transaction.
     * @throws InsufficientBalanceException If the account's balance is less than the transaction amount.
     * @throws LimitExceededException If the transaction amount would cause the account's daily limit to be exceeded.
     */
    private void validateTransaction(Account account, BigDecimal amount){
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient Balance");
        }
        BigDecimal newDailyAmount = account.getDailyTransactionAmount().add(amount);
        if(newDailyAmount.compareTo(account.getDailyTransactionLimit()) > 0){
            throw new LimitExceededException(String.format("Daily limit exceeded. Remaining limit: %.2f",
                    account.getDailyTransactionLimit().subtract(account.getDailyTransactionAmount())));
        }
    }
}