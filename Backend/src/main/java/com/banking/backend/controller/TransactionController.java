package com.banking.backend.controller;

import com.banking.backend.GlobalExceptationHandlers.TransactionProcessingException;
import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(@Valid  TransactionService transactionService){
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts",
    description = "Initiates a fund transfer from a sender account to a receiver account")
    public ResponseEntity<Void> transferFunds( @RequestBody TransactionRequestDTO request){
            transactionService.transferFunds(request);
            return ResponseEntity.ok().build();
    }
}