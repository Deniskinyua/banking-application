package com.banking.backend.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service class responsible for handling and sending various banking-related notifications
 * to an Azure Service Bus queue. This service integrates with `MessageFormatter`
 * for constructing notification messages and relies on the Azure Service Bus SDK
 * for message transmission, leveraging its built-in retry mechanisms for transient errors.
 */
@Service
public class NotificationService implements INotificationService {

    private final MessageFormatter messageFormatter;
    private final ServiceBusSenderAsyncClient serviceBusSenderAsyncClient;
    private final ServiceBusSenderAsyncClient failedNotificationSenderAsyncClient;
    private final ObjectMapper objectMapper;
    @Value("${azure.servicebus.transaction-queue-name}")
    private String queueName;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(MessageFormatter messageFormatter, ServiceBusSenderAsyncClient serviceBusSenderAsyncClient, ServiceBusSenderAsyncClient failedNotificationSenderAsyncClient, ObjectMapper objectMapper) {
        this.messageFormatter = messageFormatter;
        this.serviceBusSenderAsyncClient = serviceBusSenderAsyncClient;
        this.failedNotificationSenderAsyncClient = failedNotificationSenderAsyncClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends transfer-related notifications to both the sender and the recipient of a transaction.
     * This method constructs two distinct {@link TransactionNotification} objects (one for 'transfer out'
     * and one for 'transfer in') and dispatches them individually to the Service Bus queue.
     */
    @Override
    @Async
    public void sendTransferNotifications(String transactionId, Account sender, Account recipient, BigDecimal amount) {
        log.info("Asynchronously preparing and sending transfer notifications for transaction ID: {}", transactionId);
        String senderMessage = messageFormatter.formatSenderMessage(
                transactionId, amount, recipient.getCustomerName(), sender.getBalance(), LocalDateTime.now()
        );
        TransactionNotification senderNotification = createNotification(
                transactionId, sender.getCustomerId(), senderMessage, LocalDateTime.now(),
                TransactionType.TRANSFER_OUT.name(),
                amount, recipient.getCustomerName(), sender.getCustomerName()
        );
        sendNotificationToQueue(senderNotification);

        String recipientMessage = messageFormatter.formatRecipientMessage(
                transactionId, amount, sender.getCustomerName(), recipient.getBalance(), LocalDateTime.now()
        );
        TransactionNotification recipientNotification = createNotification(
                transactionId, recipient.getCustomerId(), recipientMessage, LocalDateTime.now(),
                TransactionType.TRANSFER_IN.name(),
                amount, recipient.getCustomerName(), sender.getCustomerName()
        );
        sendNotificationToQueue(recipientNotification);
    }

    /**
     * Asynchronously sends a {@link TransactionNotification} to the primary Azure Service Bus queue.
     * This method leverages the non-blocking {@link ServiceBusSenderAsyncClient} and
     * includes a robust error handling mechanism with a fallback to a dedicated dead-letter queue.
     */
    private void sendNotificationToQueue(TransactionNotification notification) {
        if (serviceBusSenderAsyncClient == null) {
            log.error("CRITICAL ERROR: ServiceBusSenderClient is not initialized. Cannot send notification for transaction ID: {}", notification.getTransactionId());
            throw new IllegalStateException("ServiceBusSenderClient is not initialized. Application is misconfigured or in an invalid state.");
        }

        try {
            String jsonNotification = objectMapper.writeValueAsString(notification);
            ServiceBusMessage message = new ServiceBusMessage(jsonNotification);
            message.setCorrelationId(notification.getTransactionId());

            serviceBusSenderAsyncClient.sendMessage(message)
                            .doOnSuccess(aVoid -> {
                                log.info("Sent notification to Service Bus queue '{}' for transaction ID: {} message ID: {}", queueName, notification.getTransactionId(), message.getMessageId());
                            })
                             .onErrorResume(error -> {
                                 log.error("CRITICAL ERROR: Failed to send message to Azure Service Bus queue '{}' for transaction ID: {} AFTER ALL RETRIES. Notification data: {}",
                                         queueName, notification.getTransactionId(), notification, error);

                                 return Mono.defer(() ->{
                                     try{
                                         ServiceBusMessage deadLetterMessage = new ServiceBusMessage(jsonNotification);
                                         deadLetterMessage.setCorrelationId(notification.getTransactionId());
                                         deadLetterMessage.getApplicationProperties().put("failureReason", error.getMessage());

                                         return failedNotificationSenderAsyncClient.sendMessage(deadLetterMessage)
                                                 .doOnSuccess(inform -> log.info("Moved failed notification for transaction ID: {} to failed-notifications-queue", notification.getTransactionId()))
                                                 .doOnError(deadLetterQueueError -> log.error("CRITICAL ERROR:  Failed to send message to DEAD LETTER QUEUE for transaction ID: {}. Data lost: {}", notification.getTransactionId(), notification, deadLetterQueueError))
                                                 .then();


                                     } catch (IllegalStateException deadLetterQueueError){
                                         log.error("CRITICAL ERROR: Could not send failed notification to DEAD LETTER QUEUE for transaction ID: {}. Data lost: {}", notification.getTransactionId(), notification, deadLetterQueueError);
                                         return Mono.empty();
                                     }
                                 });
                             })
                             .subscribe();
        } catch (JsonProcessingException e) {
            log.error("ERROR: Failed to serialize TransactionNotification to JSON for transaction ID: {}. Notification data: {}",
                    notification.getTransactionId(), notification, e);
            throw new NotificationSerializationException("Failed to serialize notification for transaction ID: " + notification.getTransactionId(), e);
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