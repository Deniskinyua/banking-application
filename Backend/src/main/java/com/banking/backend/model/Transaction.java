package com.banking.backend.model;

import com.banking.backend.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // Added for better @Data behavior
import lombok.ToString; // Added for better @Data behavior

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity

@EqualsAndHashCode(exclude = "account") // Exclude lazy-loaded parent from equals/hashCode
@ToString(exclude = "account") // Exclude lazy-loaded parent from toString
public class Transaction {
    @Id
    @GeneratedValue
    private String transactionId; // Assuming transactionId is generated and unique

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 255) // Max length for description
    private String description;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY) // Always lazy load ManyToOne
    @JoinColumn(name = "account_id", nullable = false) // Ensure account_id is not null
    private Account account;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}