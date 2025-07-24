package com.banking.backend.azure.messagelistener;

import com.azure.messaging.servicebus.ServiceBusClientBuilder; // <-- ADD THIS IMPORT
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;

import com.banking.backend.dto.TransactionNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);

    // Inject the Builder instead of the client
    private final ServiceBusClientBuilder.ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder;
    private final ObjectMapper objectMapper;

    // The actual processor client instance
    private ServiceBusProcessorClient serviceBusProcessorClient; // No longer final, initialized in @PostConstruct

    // Manual constructor for dependency injection of the builder and ObjectMapper
    public NotificationProcessor(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder, ObjectMapper objectMapper) {
        this.serviceBusProcessorClientBuilder = serviceBusProcessorClientBuilder;
        this.objectMapper = objectMapper;
    }


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

    // --- Message Handling Logic ---
    public void handleMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        try {
            String messageBody = message.getBody().toString();
            log.debug("Received message from Service Bus (Sequence #{}): {}", message.getSequenceNumber(), messageBody);

            TransactionNotification notification = objectMapper.readValue(messageBody, TransactionNotification.class);
            processNotification(notification);

            context.complete();
            log.info("Successfully processed and completed message for transaction ID: {}", notification.getTransactionId());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to deserialize message body to TransactionNotification. Message will be dead-lettered. Message body: {}", message.getBody().toString(), e);
            context.deadLetter();
        } catch (Exception e) {
            log.error("Error processing message from Service Bus. Message will be abandoned. Message body: {}. Sequence #{}", message.getBody().toString(), message.getSequenceNumber(), e);
            context.abandon();
        }
    }

    // --- Error Handling Logic ---
    public void handleError(ServiceBusErrorContext context) {
        log.error("Error occurred while processing message from Service Bus. Entity path: {}, Error source: {}, Exception: {}",
                context.getEntityPath(), context.getErrorSource(), context.getException());
    }

    private void processNotification(TransactionNotification notification) {
        log.info("Starting processing for notification: {}", notification.getTransactionId());
        log.info("Notification details: Transaction ID: {}, User ID: {}, Message: {}",
                notification.getTransactionId(), notification.getUserId(), notification.getMessage());
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

    @PreDestroy
    public void stopListening() {
        if (serviceBusProcessorClient != null) {
            log.info("Stopping Service Bus Processor Client...");
            serviceBusProcessorClient.stop();
            log.info("Service Bus Processor Client stopped.");
        }
    }
}