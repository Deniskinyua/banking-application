package com.banking.backend.controller;

import com.banking.backend.GlobalExceptationHandlers.GlobalExceptionHandler;
import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing financial transactions.
 * This class handles incoming HTTP requests related to fund transfers
 *
 * @author Denis Kinyua
 * @version 1.0
 * @since 10/07/2025
 */
@Tag(name = "Transaction Management API",
description = "REST APIs for managing funds transfer operations among users within the banking system")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    /**
     * Constructor for TransactionController
     * @param transactionService The service layer for handling transaction business logic
     */
    public TransactionController(TransactionService transactionService){
        this.transactionService = transactionService;
    }

    /**
     * Initiates funds transfer between two accounts
     * This endpoint processes a request to move a specified amount from a sender's account to a receiver's account
     * @param request The TransactionDTO containing details of the transfer (sender, receiver, amount)
     * The request body is validated against defined DTO constraints.
     * @return A ResponseEntity indicating the outcome of the transfer.
     * - 200 OK : Funds successfully transferred.
     * - 400 BAD REQUEST : Invalid request (Insufficient funds, Invalid account numbers, daily limit exceeded)
     * - 500 INTERNAL SERVER ERROR : Unexpected server-side issues during the transaction.
     */
    @PostMapping("/transfer")
    @Operation(method = "POST", summary = "Transfer funds between accounts",
    description = "Initiates a fund transfer from a sender account to a receiver account")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Funds transferred successfully",
                    content = @Content(schema = @Schema (implementation = void.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or transaction processing error(validation failure, Insufficient funds, daily limit exceeded, account not found)",
                    content = @Content(schema = @Schema (implementation = GlobalExceptionHandler.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal Server Error during transfer",
                    content = @Content(schema = @Schema(implementation = Exception.class))
            )
    })
    public ResponseEntity<Void> transferFunds( @Valid @RequestBody TransactionRequestDTO request){
            transactionService.transferFunds(request);
            return ResponseEntity.ok().build();
    }
}