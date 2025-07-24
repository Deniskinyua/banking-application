package com.banking.backend.config.servicebusconfig;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient; // Still import if you use ServiceBusClientBuilder.processor()
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
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

    @Value("${azure.servicebus.queue-name}")
    private String queueName;

    @Bean
    public ServiceBusSenderClient serviceBusSenderClient() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

    @Bean
    public ServiceBusReceiverClient serviceBusReceiverClient() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .disableAutoComplete()
                .buildClient();
    }

    // NEW BEAN: Provide the ServiceBusProcessorClientBuilder
    @Bean
    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder serviceBusProcessorClientBuilder() {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName);
        // Do NOT call .processMessage(), .processError(), or .buildProcessorClient() here.
        // These will be called in NotificationProcessor.
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}