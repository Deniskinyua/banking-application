package com.banking.backend.GlobalExceptationHandlers;

public class LimitExceededException extends RuntimeException { // Changed to RuntimeException

    public LimitExceededException(String message) {
        super(message);
    }

    public LimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}