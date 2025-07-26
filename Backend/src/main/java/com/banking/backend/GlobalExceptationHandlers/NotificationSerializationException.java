package com.banking.backend.GlobalExceptationHandlers;

public class NotificationSerializationException extends RuntimeException {
    public NotificationSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}