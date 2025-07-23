package com.banking.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionNotification {
    private String transactionId;
    private String userId;
    private  String message;
    private LocalDateTime timestamp;
}
