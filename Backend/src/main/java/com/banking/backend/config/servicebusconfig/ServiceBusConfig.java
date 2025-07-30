package com.banking.backend.config.servicebusconfig;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.ServiceBusClientBuilder.ServiceBusProcessorClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfig {

    @Value("${spring.cloud.azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.transaction-queue-name}")
    private String queueName;

    @Value("${azure.servicebus.failed-transactions-queue}")
    private  String failedTransactionQueueName;

    /**
     * Configures and provides an asynchronous Service Bus sender client for the primary queue.
     * This client is used to send messages to the main Service Bus queue.
     * @return A ServiceBusSenderAsyncClient instance.
     */
    @Bean
    public ServiceBusSenderAsyncClient serviceBusSenderAsyncClient() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildAsyncClient();
    }

    /**
     * Configures and provides an asynchronous Service Bus receiver client for the primary queue.
     * This client is used for manual (peek-lock) message reception from the main queue,
     * allowing explicit settlement (complete, abandon, dead-letter) of messages.
     * @return A ServiceBusReceiverAsyncClient instance.
     */
    @Bean
    public ServiceBusReceiverAsyncClient serviceBusReceiverAsyncClient() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .disableAutoComplete() // Disables automatic message completion/abandonment
                .buildAsyncClient();
    }

    /**
     * Provides a builder for configuring a Service Bus processor client for the primary queue.
     * This builder is used to set up a push-based message processing mechanism, typically with
     * defined message and error handlers.
     * @return A ServiceBusProcessorClientBuilder instance.
     */
    @Bean
    public ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName);
    }

    /**
     * Configures and provides an asynchronous Service Bus sender client specifically for the
     * dead-letter queue (DLQ) for failed notifications. This client is used to send messages
     * that could not be successfully processed to a dedicated queue for further analysis.
     * @param failedTransactionQueueName The name of the failed transactions queue, injected via @Value.
     * @return A ServiceBusSenderAsyncClient instance for the DLQ.
     */
    @Bean
    public ServiceBusSenderAsyncClient failedNotificationSenderAsyncClient(@Value("${azure.servicebus.failed-transactions-queue}")String failedTransactionQueueName){
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(failedTransactionQueueName)
                .buildAsyncClient();
    }

    /**
     * Configures and provides an ObjectMapper bean for JSON serialization and deserialization.
     * It registers the JavaTimeModule to correctly handle Java 8 Date and Time API types
     * and disables writing dates as timestamps, preferring ISO 8601 format.
     * @return A configured ObjectMapper instance.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}