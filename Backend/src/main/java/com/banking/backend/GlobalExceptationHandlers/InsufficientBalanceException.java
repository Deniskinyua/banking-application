package com.banking.backend.GlobalExceptationHandlers;

public class InsufficientBalanceException extends Throwable {

    public InsufficientBalanceException(String insufficientBalance) {
        super(insufficientBalance);
    }
}
