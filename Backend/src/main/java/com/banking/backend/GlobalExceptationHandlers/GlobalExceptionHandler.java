package com.banking.backend.GlobalExceptationHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the banking application.
 * This class catches specific exceptions thrown by controllers and services
 * and returns appropriate HTTP responses, centralizing error handling logic.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    public static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles exceptions related to insufficient balance.
     * Returns HTTP 400 Bad Request.
     *
     * @param ex The InsufficientBalanceException thrown.
     * @return A ResponseEntity with BAD_REQUEST status and the exception message.
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<String> handleInsufficientBalance(InsufficientBalanceException ex){
        log.warn("InsufficientBalanceException caught: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Handles exceptions related to daily transaction limit being exceeded.
     * Returns HTTP 400 Bad Request.
     *
     * @param ex The LimitExceededException thrown.
     * @return A ResponseEntity with BAD_REQUEST status and the exception message.
     */
    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<String> handleLimitExceeded(LimitExceededException ex){
        log.warn("LimitExceededException caught: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Handles general transaction processing exceptions.
     * This can be used for any business rule violation during transaction operations
     * that doesn't fit into more specific exceptions like InsufficientBalanceException.
     * Returns HTTP 400 Bad Request.
     *
     * @param ex The TransactionProcessingException thrown.
     * @return A ResponseEntity with BAD_REQUEST status and the exception message.
     */
    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<String> handleTransactionProcessingException(TransactionProcessingException ex){
        log.warn("TransactionProcessingException caught: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Handles illegal argument exceptions, typically for invalid inputs or state.
     * Returns HTTP 400 Bad Request.
     *
     * @param ex The IllegalArgumentException thrown.
     * @return A ResponseEntity with BAD_REQUEST status and the exception message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex){
        log.warn("IllegalArgumentException caught: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Handles validation errors when request body arguments are not valid (e.g., from @Valid).
     * Returns HTTP 400 Bad Request with details about the validation failure.
     *
     * @param ex The MethodArgumentNotValidException thrown.
     * @return A ResponseEntity with BAD_REQUEST status and a detailed validation error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst() // You might want to collect all errors here if multiple
                .orElse("Validation failed");
        log.warn("MethodArgumentNotValidException caught: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation Error: " + errorMessage);
    }

    /**
     * Catches any other unhandled exceptions.
     * Returns HTTP 500 Internal Server Error and a generic error message.
     * Logs the full stack trace for debugging purposes.
     *
     * @param ex The unhandled Exception.
     * @return A ResponseEntity with INTERNAL_SERVER_ERROR status and a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex){
        log.error("An unhandled exception occurred: {}", ex.getMessage(), ex); // Log full stack trace
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again later.");
    }
}