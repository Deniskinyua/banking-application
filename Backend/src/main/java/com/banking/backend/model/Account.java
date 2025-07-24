package com.banking.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "Account")
@EqualsAndHashCode(exclude = "transactions") // Exclude lazy-loaded collections from equals/hashCode
@ToString(exclude = "transactions") // Exclude lazy-loaded collections from toString
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // Add constraints
    private String accountNumber;

    private String customerName;

    @Column(unique = true, nullable = false) // Add constraints
    private String customerId; // Should be distinct from accountNumber if used as primary identifier

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO; // Initialize to zero

    @Column(nullable = false)
    private BigDecimal dailyTransactionLimit = BigDecimal.valueOf(500000.00); // Sensible default

    @Column(nullable = false)
    private BigDecimal dailyTransactionAmount = BigDecimal.ZERO; // Initialize to zero

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // Default to LAZY
    private List<Transaction> transactions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getDailyTransactionLimit() {
        return dailyTransactionLimit;
    }

    public void setDailyTransactionLimit(BigDecimal dailyTransactionLimit) {
        this.dailyTransactionLimit = dailyTransactionLimit;
    }

    public BigDecimal getDailyTransactionAmount() {
        return dailyTransactionAmount;
    }

    public void setDailyTransactionAmount(BigDecimal dailyTransactionAmount) {
        this.dailyTransactionAmount = dailyTransactionAmount;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(Transaction transaction){
        if (this.transactions == null) { // Defensive check
            this.transactions = new ArrayList<>();
        }
        transactions.add(transaction);
        transaction.setAccount(this);
    }

    // You might want to add getters/setters for specific logic if @Data is too broad for JPA entities
    // For example, if you want to control how balance is updated.
}