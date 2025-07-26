package com.banking.backend.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.banking.backend.GlobalExceptationHandlers.NotificationSerializationException;
import com.banking.backend.dto.TransactionNotification;
import com.banking.backend.enums.TransactionType;
import com.banking.backend.model.Account;
import com.banking.backend.service.interfaces.INotificationService;
import com.banking.backend.util.MessageFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service class responsible for handling and sending various banking-related notifications
 * to an Azure Service Bus queue. This service integrates with `MessageFormatter`
 * for constructing notification messages and relies on the Azure Service Bus SDK
 * for message transmission, leveraging its built-in retry mechanisms for transient errors.
 */
@Service
// @Slf4j // Uncomment this line if you prefer Lombok's logger injection over manual LoggerFactory.getLogger
public class NotificationService implements INotificationService {

    private final MessageFormatter messageFormatter;

   //Azure Service Bus sender client for dispatching messages to the configured queue.
    private final ServiceBusSenderClient serviceBusSenderClient;
    private final ObjectMapper objectMapper;
    @Value("${azure.servicebus.queue-name}")
    private String queueName;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(MessageFormatter messageFormatter, ServiceBusSenderClient serviceBusSenderClient, ObjectMapper objectMapper) {
        this.messageFormatter = messageFormatter;
        this.serviceBusSenderClient = serviceBusSenderClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends transfer-related notifications to both the sender and the recipient of a transaction.
     * This method constructs two distinct {@link TransactionNotification} objects (one for 'transfer out'
     * and one for 'transfer in') and dispatches them individually to the Service Bus queue.
     */
    @Override // Good practice to explicitly state override for interface methods
    public void sendTransferNotifications(String transactionId, Account sender, Account recipient, BigDecimal amount) {
        // Prepare and send notification for the sender (transfer out)
        String senderMessage = messageFormatter.formatSenderMessage(
                transactionId, amount, recipient.getCustomerName(), sender.getBalance(), LocalDateTime.now()
        );
        TransactionNotification senderNotification = createNotification(
                transactionId, sender.getCustomerId(), senderMessage, LocalDateTime.now(),
                TransactionType.TRANSFER_OUT.name(), // Use .name() for enum string representation
                amount, recipient.getCustomerName(), sender.getCustomerName()
        );
        sendNotificationToQueue(senderNotification);

        // Prepare and send notification for the recipient (transfer in)
        String recipientMessage = messageFormatter.formatRecipientMessage(
                transactionId, amount, sender.getCustomerName(), recipient.getBalance(), LocalDateTime.now()
        );
        TransactionNotification recipientNotification = createNotification(
                transactionId, recipient.getCustomerId(), recipientMessage, LocalDateTime.now(),
                TransactionType.TRANSFER_IN.name(), // Use .name() for enum string representation
                amount, recipient.getCustomerName(), sender.getCustomerName()
        );
        sendNotificationToQueue(recipientNotification);
    }

    /**
     * Internal helper method to send a single {@link TransactionNotification} object to the
     * configured Azure Service Bus queue. This method handles JSON serialization and delegates
     * message sending to the {@link ServiceBusSenderClient}.
     *
     * <p>The {@code serviceBusSenderClient} is expected to have built-in retry logic (e.g., exponential backoff)
     * configured by Spring Cloud Azure for transient errors. Therefore, explicit retry loops are not
     * implemented here for network/service-related issues.</p>
     */
    private void sendNotificationToQueue(TransactionNotification notification) {
        if (serviceBusSenderClient == null) {
            log.error("CRITICAL ERROR: ServiceBusSenderClient is not initialized. Cannot send notification for transaction ID: {}", notification.getTransactionId());
            throw new IllegalStateException("ServiceBusSenderClient is not initialized. Application is misconfigured or in an invalid state.");
        }

        try {
            String jsonNotification = objectMapper.writeValueAsString(notification);
            ServiceBusMessage message = new ServiceBusMessage(jsonNotification);
            // Set the correlation ID for traceability in Service Bus.
            message.setCorrelationId(notification.getTransactionId());

            serviceBusSenderClient.sendMessage(message);

            log.info("Sent notification to Service Bus queue '{}' for transaction ID: {} message ID: {}", queueName, notification.getTransactionId(), message.getMessageId());
            log.debug("Full message sent: {}", jsonNotification);
        } catch (JsonProcessingException e) {
            log.error("ERROR: Failed to serialize TransactionNotification to JSON for transaction ID: {}. Notification data: {}",
                    notification.getTransactionId(), notification, e);
            throw new NotificationSerializationException("Failed to serialize notification for transaction ID: " + notification.getTransactionId(), e);
        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to send message to Azure Service Bus queue '{}' for transaction ID: {} AFTER ALL RETRIES. Notification data: {}",
                    queueName, notification.getTransactionId(), notification, e);
            throw new RuntimeException("Failed to send notification to Azure Service Bus for transaction ID: " + notification.getTransactionId(), e);
        }
    }

    /**
     * Creates a {@link TransactionNotification} object with the provided details.
     * This is a helper method to centralize notification object creation.
     */
    private TransactionNotification createNotification(String transactionId, String customerId, String message, LocalDateTime timestamp,
                                                       String transactionType, BigDecimal amount, String recipientName, String senderName) {
        TransactionNotification notification = new TransactionNotification();
        notification.setTransactionId(transactionId);
        notification.setUserId(customerId);
        notification.setMessage(message);
        notification.setTimestamp(timestamp);
        notification.setTransactionType(transactionType);
        notification.setAmount(amount);
        notification.setRecipientName(recipientName);
        notification.setSenderName(senderName);
        return notification;
    }
}