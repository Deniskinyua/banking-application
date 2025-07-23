package com.banking.backend.controller;

import com.banking.backend.dto.TransactionRequestDTO;
import com.banking.backend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferFunds(@RequestBody TransactionRequestDTO request){
        transactionService.transferFunds(request);
        return ResponseEntity.ok().build();
    }
}
