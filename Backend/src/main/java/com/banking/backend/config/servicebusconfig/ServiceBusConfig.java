package com.banking.backend.config.servicebusconfig;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.ServiceBusClientBuilder.ServiceBusProcessorClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfig {

    @Value("${spring.cloud.azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.queue-name}")
    private String queueName;

    @Value("${azure.servicebus.failed-transactions-queue}")
    private  String failedTransactionQueueName;

    @Bean
    public ServiceBusSenderAsyncClient serviceBusSenderAsyncClient() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildAsyncClient();
    }

    @Bean
    public ServiceBusReceiverAsyncClient serviceBusReceiverAsyncClient() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .disableAutoComplete()
                .buildAsyncClient();
    }

    @Bean
    public ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName);
    }

    @Bean
    public ServiceBusSenderAsyncClient failedNotificationSenderAsyncClient(@Value("${azure.servicebus.failed-transactions-queue}")String failedTransactionQueueName){
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(failedTransactionQueueName)
                .buildAsyncClient();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}