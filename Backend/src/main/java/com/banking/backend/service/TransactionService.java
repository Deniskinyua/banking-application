package com.banking.backend.service;

import com.banking.backend.GlobalExceptationHandlers.InsufficientBalanceException;
import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.enums.TransactionType;
import com.banking.backend.model.Account;
import com.banking.backend.model.Transaction;
import com.banking.backend.repository.AccountRepository;
import com.banking.backend.util.TransactionIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.naming.LimitExceededException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    public void transferFunds(TransactionRequestDTO request) throws InsufficientBalanceException, LimitExceededException {
        Account fromAccount = accountRepository.findByUserId(request.getFromUserId()).orElseThrow(() ->
                new RuntimeException("sender account not found"));
        Account toAccount = accountRepository.findByUserId(request.getToUserId()).orElseThrow(() ->
                new RuntimeException("recipient account not found"));

        validateTransaction(fromAccount, request.getAmount());
        String transactionId = TransactionIdGenerator.generate();

        //Debit sender account
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        fromAccount.setDailyTransactionAmount(
                fromAccount.getDailyTransactionAmount().add(request.getAmount())
        );

        Transaction debitTransaction = createTransaction(transactionId,
                TransactionType.TRANSFER_OUT, request.getAmount().negate(),
                String.format("Transfer to %s", toAccount.getCustomerName()),
                LocalDateTime.now(), fromAccount.getBalance());

        fromAccount.addTransaction(debitTransaction);

        //Credit recipient account;
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        Transaction creditTransaction = createTransaction(
                transactionId, TransactionType.TRANSFER_IN, request.getAmount(),
                String.format("Transfer from %s", fromAccount.getCustomerName()), LocalDateTime.now(),
                toAccount.getBalance()
        );
        toAccount.addTransaction(creditTransaction);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        //Send Notification
        notificationService.sendTransferNotifications(transactionId, fromAccount, toAccount, request.getAmount());
    }

    private void validateTransaction(Account account, BigDecimal amount) throws InsufficientBalanceException, LimitExceededException {
        if(account.getBalance().compareTo(amount) < 0){
            throw new InsufficientBalanceException("Insufficient Balance");
        }
        BigDecimal newDailyAmount = account.getDailyTransactionAmount().add(amount);
        if(newDailyAmount.compareTo(account.getDailyTransactionLimit()) > 0){
            throw new LimitExceededException("Daily limit exceeded");
        }
    }

    private Transaction createTransaction(String id,TransactionType type, BigDecimal amount,
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
}
