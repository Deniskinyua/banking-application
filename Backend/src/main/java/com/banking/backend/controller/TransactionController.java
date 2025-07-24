package com.banking.backend.controller;

import com.banking.backend.GlobalExceptationHandlers.TransactionProcessingException;
import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid; // For DTO validation, if using Spring Validation

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService){
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferFunds( @RequestBody TransactionRequestDTO request){ // Added @Valid
        try {
            transactionService.transferFunds(request);
            return ResponseEntity.ok().build();
        } catch (TransactionProcessingException e) {
            // Specific handling for business exceptions from the service layer
            // The GlobalExceptionHandler will catch this and return a BAD_REQUEST
            throw e; // Re-throw to be caught by GlobalExceptionHandler
        } catch (Exception e) {
            // Catch any unexpected exceptions and re-throw as a generic runtime error
            // to be caught by GlobalExceptionHandler and return INTERNAL_SERVER_ERROR
            throw new RuntimeException("An unexpected error occurred during fund transfer.", e);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Void> testing(@RequestBody String testModel){
        System.out.println("testModel");
        return ResponseEntity.ok().build();
    }
}