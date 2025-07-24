package com.banking.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor; // Added NoArgsConstructor for Jackson deserialization

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor // Essential for Jackson to create an instance during deserialization
public class TransactionNotification implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes
    private String transactionId;
    private String userId;
    private String message;
    private LocalDateTime timestamp;
    private String transactionType; // E.g., "DEBIT", "CREDIT" for clarity
    private BigDecimal amount;
    private String recipientName;
    private String senderName;

    // Consider adding a constructor for convenience,
    // if you frequently create these objects with all fields.
    // public TransactionNotification(String transactionId, String userId, ...) { ... }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }


}