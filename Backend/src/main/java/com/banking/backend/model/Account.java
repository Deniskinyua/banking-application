package com.banking.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String customerName;
    private String customerId;
    private BigDecimal balance;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal dailyTransactionAmount;
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();
    public void addTransaction(Transaction transaction){
        transactions.add(transaction);
        transaction.setAccount(this);
    }
}
