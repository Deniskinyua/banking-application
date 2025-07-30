package com.banking.backend.azure.messagelistener;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;

import com.banking.backend.dto.TransactionNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);
    private final ServiceBusClientBuilder.ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder;
    private final ObjectMapper objectMapper;
    private ServiceBusProcessorClient serviceBusProcessorClient;

    public NotificationProcessor(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder, ObjectMapper objectMapper) {
        this.serviceBusProcessorClientBuilder = serviceBusProcessorClientBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes and starts the Azure Service Bus Processor Client.
     * This method is automatically called by Spring after the bean's construction and dependency injection are complete.
     * It configures the message and error handlers and then starts listening for messages on the configured queue/topic.
     */
    @PostConstruct
    public void startListening() {
        log.info("Starting Service Bus Processor Client to listen for messages...");
        // Build the client here, providing handlers from this class instance
        this.serviceBusProcessorClient = serviceBusProcessorClientBuilder
                .processMessage(this::handleMessage)
                .processError(this::handleError)
                .buildProcessorClient();

        serviceBusProcessorClient.start();
        log.info("Service Bus Processor Client started.");
    }

    /**
     * Handles a received message from Azure Service Bus.
     * This method is invoked by the Service Bus Processor Client for each message received.
     * It deserializes the message body into a {@link TransactionNotification} object,
     * processes it, and then completes the message on the Service Bus.
     * If deserialization fails, the message is dead-lettered. If any other error occurs during processing,
     * the message is abandoned, making it available for re-delivery.
     *
     * @param context The {@link ServiceBusReceivedMessageContext} containing the received message and completion/abandonment controls.
     */
    public void handleMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        try {
            String messageBody = message.getBody().toString();
            log.debug("Received message from Service Bus (Sequence #{}): {}", message.getSequenceNumber(), messageBody);

            TransactionNotification notification = objectMapper.readValue(messageBody, TransactionNotification.class);
            processNotification(notification);

            context.complete();
            log.info("Successfully processed and completed message for transaction ID: {}", notification.getTransactionId());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message body to TransactionNotification. Message will be dead-lettered. Message body: {}", message.getBody().toString(), e);
            context.deadLetter();
        } catch (Exception e) {
            log.error("Error processing message from Service Bus. Message will be abandoned. Message body: {}. Sequence #{}", message.getBody().toString(), message.getSequenceNumber(), e);
            context.abandon();
        }
    }

    /**
     * Handles errors that occur during message processing by the Service Bus Processor Client.
     * This method provides a centralized point for logging and reacting to Service Bus-related errors,
     * such as connection issues, unauthorized access, or internal Service Bus errors.
     *
     * @param context The {@link ServiceBusErrorContext} providing details about the error,
     * including the entity path, error source, and the exception itself.
     */
    public void handleError(ServiceBusErrorContext context) {
        log.error("Error occurred while processing message from Service Bus. Entity path: {}, Error source: {}, Exception: {}",
                context.getEntityPath(), context.getErrorSource(), context.getException());
    }

    /**
     * Processes the received {@link TransactionNotification}.
     * This method contains the core business logic for handling a transaction notification.
     * In a real application, this would involve more complex operations like updating a database,
     * sending emails, or triggering other downstream services.
     *
     * @param notification The {@link TransactionNotification} object parsed from the Service Bus message.
     */
    private void processNotification(TransactionNotification notification) {
        log.info("Starting processing for notification: {}, Notification details: Transaction ID: {}, User ID: {}, Message: {}",
                notification.getTransactionId(), notification.getTransactionId(), notification.getUserId(), notification.getMessage());

        // System.out.println for immediate console output, typically replaced with a more robust notification system
        System.out.println("----**NEW NOTIFICATION**----");
        System.out.println("Transaction ID: " + notification.getTransactionId());
        System.out.println("User ID: " + notification.getUserId());
        System.out.println("Message: " + notification.getMessage());
        System.out.println("Timestamp: " + notification.getTimestamp());
        System.out.println("Transaction Type: " + notification.getTransactionType());
        System.out.println("Amount: " + notification.getAmount());
        if (notification.getRecipientName() != null) {
            System.out.println("Recipient Name: " + notification.getRecipientName());
        }
        if (notification.getSenderName() != null) {
            System.out.println("Sender Name: " + notification.getSenderName());
        }
        System.out.println("-------------------------");
    }

    /**
     * Stops the Azure Service Bus Processor Client.
     * This method is automatically called by Spring when the application context is gracefully shutting down.
     * It ensures that the client releases its resources and stops listening for messages, preventing
     * message loss or incomplete processing during shutdown.
     */
    @PreDestroy
    public void stopListening() {
        if (serviceBusProcessorClient != null) {
            log.info("Stopping Service Bus Processor Client...");
            serviceBusProcessorClient.stop();
            log.info("Service Bus Processor Client stopped.");
        }
    }
}