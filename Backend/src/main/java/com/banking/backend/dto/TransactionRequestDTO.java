package com.banking.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequestDTO {
    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;
    private String description;
}
