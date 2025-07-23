package com.banking.backend.service;

import com.banking.backend.dto.TransactionNotification;
import com.banking.backend.model.Account;
import com.banking.backend.util.MessageFormatter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final ServiceBusTemplate serviceBusTemplate;
    private final MessageFormatter messageFormatter;

    public void sendTransferNotifications(String transactionId, Account sender, Account recipient, BigDecimal amount) {
        //Send notification to sender
        String senderMessage = messageFormatter.formatSenderMessage(
                transactionId, amount, recipient.getCustomerName(), sender.getBalance(), LocalDateTime.now()
        );
        sendNotification(transactionId, sender.getCustomerId(), senderMessage, LocalDateTime.now());

        //Send notification to recipient
        String recipientMessage = messageFormatter.formatRecipientMessage(
                transactionId, amount, sender.getCustomerName(), recipient.getBalance(), LocalDateTime.now()
        );
        sendNotification(transactionId, recipient.getCustomerId(), recipientMessage, LocalDateTime.now());
    }

    private void sendNotification(String transactionId, String customerId, String message, LocalDateTime timestamp) {
        TransactionNotification notification = new TransactionNotification();
        notification.setTransactionId(transactionId);
        notification.setUserId(customerId);
        notification.setMessage(message);
        notification.setTimestamp(timestamp);

        serviceBusTemplate.sendAsync("transaction-notifications", MessageBuilder.withPayload(notification).build());
    }
}
