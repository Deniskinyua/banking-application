package com.banking.backend.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.banking.backend.dto.TransactionNotification;
import com.banking.backend.enums.TransactionType; // Import TransactionType enum
import com.banking.backend.model.Account;
import com.banking.backend.util.MessageFormatter;
import com.fasterxml.jackson.core.JsonProcessingException; // Use specific Jackson exception
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final MessageFormatter messageFormatter;
    private final ServiceBusSenderClient serviceBusSenderClient;
    private final ObjectMapper objectMapper;
    @Value("${azure.servicebus.queue-name}")
    private String queueName;
    private static Logger log = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(MessageFormatter messageFormatter, ServiceBusSenderClient serviceBusSenderClient, ObjectMapper objectMapper){
        this.messageFormatter = messageFormatter;
        this.serviceBusSenderClient = serviceBusSenderClient;
        this.objectMapper = objectMapper;
    }

    public void sendTransferNotifications(String transactionId, Account sender, Account recipient, BigDecimal amount) {
        // Prepare sender notification
        String senderMessage = messageFormatter.formatSenderMessage(
                transactionId, amount, recipient.getCustomerName(), sender.getBalance(), LocalDateTime.now()
        );
        TransactionNotification senderNotification = createNotification(
                transactionId, sender.getCustomerId(), senderMessage, LocalDateTime.now(),
                TransactionType.TRANSFER_OUT.name(), // Use enum name for consistency
                amount, recipient.getCustomerName(), sender.getCustomerName() // Include sender name for better context
        );
        sendNotificationToQueue(senderNotification);

        // Prepare recipient notification
        String recipientMessage = messageFormatter.formatRecipientMessage(
                transactionId, amount, sender.getCustomerName(), recipient.getBalance(), LocalDateTime.now()
        );
        TransactionNotification recipientNotification = createNotification(
                transactionId, recipient.getCustomerId(), recipientMessage, LocalDateTime.now(),
                TransactionType.TRANSFER_IN.name(), // Use enum name for consistency
                amount, recipient.getCustomerName(), sender.getCustomerName() // Include recipient name for better context
        );
        sendNotificationToQueue(recipientNotification);
    }

    private void sendNotificationToQueue(TransactionNotification notification) {
        if (serviceBusSenderClient == null) {
            log.error("ServiceBusSenderClient is not initialized. Cannot send notification: {}", notification.getTransactionId());
            // Depending on criticality, you might throw an exception,
            // or trigger an alert here.
            return;
        }

        try {
            String jsonNotification = objectMapper.writeValueAsString(notification);
            ServiceBusMessage message = new ServiceBusMessage(jsonNotification);
            // Consider adding correlation ID or message ID for tracing
            message.setCorrelationId(notification.getTransactionId());
            // message.setMessageId(UUID.randomUUID().toString()); // If you need a unique message ID

            serviceBusSenderClient.sendMessage(message);
            log.info("Sent notification to Service Bus queue '{}' for transaction ID: {}", queueName, notification.getTransactionId());
            log.debug("Full message sent: {}", jsonNotification); // Debug log for full payload
        } catch (JsonProcessingException e) { // Use specific exception from com.fasterxml.jackson.core
            log.error("Error serializing TransactionNotification to JSON for transaction ID: {}. Notification data: {}", notification.getTransactionId(), notification, e);
            // This is a data serialization error. It implies a bug in the DTO or ObjectMapper config.
            // You might want to throw a custom runtime exception here to indicate a critical issue
            // rather than silently logging and continuing.
            throw new RuntimeException("Failed to serialize notification for transaction ID: " + notification.getTransactionId(), e);
        } catch (Exception e) {
            log.error("Error sending message to Azure Service Bus queue '{}' for transaction ID: {}. Notification data: {}",
                    queueName, notification.getTransactionId(), notification, e);
            // This could be a transient network error or Service Bus availability issue.
            // Consider implementing retry logic here if not handled by Spring Cloud Azure.
            // For now, rethrowing as a runtime exception to bubble up if it's critical for the transaction to complete.
            throw new RuntimeException("Failed to send notification to Azure Service Bus for transaction ID: " + notification.getTransactionId(), e);
        }
    }

    private TransactionNotification createNotification(String transactionId, String customerId, String message, LocalDateTime timestamp,
                                                       String transactionType, BigDecimal amount, String recipientName, String senderName){
        TransactionNotification notification = new TransactionNotification();
        notification.setTransactionId(transactionId);
        notification.setUserId(customerId);
        notification.setMessage(message);
        notification.setTimestamp(timestamp);
        notification.setTransactionType(transactionType);
        notification.setAmount(amount);
        notification.setRecipientName(recipientName);
        notification.setSenderName(senderName); // Make sure senderName is passed for both notifications if needed
        return notification;
    }
}