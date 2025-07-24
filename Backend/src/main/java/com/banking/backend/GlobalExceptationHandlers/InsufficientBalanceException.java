package com.banking.backend.GlobalExceptationHandlers;

// It's generally better for custom exceptions that map to HTTP errors
// to extend RuntimeException rather than Throwable.
// This allows them to be unchecked and propagate naturally without
// requiring explicit 'throws' clauses everywhere, while still being
// caught by @ControllerAdvice.
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    // You might also add a constructor that takes a Throwable cause
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}