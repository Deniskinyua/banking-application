package com.banking.backend.GlobalExceptationHandlers;


/**
 * Custom exception to indicate a business rule violation during transaction processing.
 * This is an unchecked exception (extends RuntimeException) to avoid cluttering method signatures,
 * and it will be caught by the GlobalExceptionHandler to return an appropriate HTTP 4xx response.
 */
public class TransactionProcessingException extends RuntimeException {

    /**
     * Constructs a new TransactionProcessingException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     */
    public TransactionProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new TransactionProcessingException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the getCause() method).
     * (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}