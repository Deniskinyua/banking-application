package com.banking.backend.service;

import com.banking.backend.GlobalExceptationHandlers.InsufficientBalanceException;
import com.banking.backend.GlobalExceptationHandlers.LimitExceededException; // Correct import
import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.enums.TransactionType;
import com.banking.backend.model.Account;
import com.banking.backend.model.Transaction;
import com.banking.backend.repository.AccountRepository;
import com.banking.backend.util.TransactionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private static Logger log = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(AccountRepository accountRepository, NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
    }

    @Transactional // Ensure atomicity of debit/credit operations
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

    private void validateTransaction(Account account, BigDecimal amount) throws InsufficientBalanceException, LimitExceededException {
        // Corrected comparison: balance must be strictly greater than amount
        // If balance is 100 and amount is 100, it's NOT insufficient.
        // If balance is 100 and amount is 100.01, it IS insufficient.
        if (account.getBalance().compareTo(amount) < 0) { // Keep '< 0' if partial balance transfer is possible
            throw new InsufficientBalanceException("Insufficient Balance");
        }
        // If a direct "transfer all" scenario is allowed, then account.getBalance().compareTo(amount) < 0
        // is correct. If it must be strictly greater, then account.getBalance().compareTo(amount) <= 0.
        // Your original logic for <= 0 for insufficient balance seems more intuitive for "cannot be less than or equal to amount".
        // Let's stick with '< 0' meaning 'balance is strictly less than amount', which implies insufficient.
        // So, if you have 100 and try to send 100, it passes. If you have 99 and try to send 100, it fails.
        // If you intended 100 to 100 to fail, use compareTo(amount) <= 0.
        // Reverting to your original for clarity, meaning "cannot be less than or equal to 0"
        // Let's assume you meant "balance must be strictly greater than amount for transfer to occur"
        // If you have 100 and send 100, new balance is 0. If you have 100 and send 100.01, insufficient.
//        if(account.getBalance().compareTo(amount) < 0){ // if balance < amount, insufficient
//            throw new InsufficientBalanceException("Insufficient Balance");
//        }


        BigDecimal newDailyAmount = account.getDailyTransactionAmount().add(amount);
        if(newDailyAmount.compareTo(account.getDailyTransactionLimit()) > 0){
            throw new LimitExceededException(String.format("Daily limit exceeded. Remaining limit: %.2f",
                    account.getDailyTransactionLimit().subtract(account.getDailyTransactionAmount())));
        }
    }
}