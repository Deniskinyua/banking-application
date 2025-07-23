package com.banking.backend.GlobalExceptationHandlers;

public class LimitExceededException extends Throwable {

    public LimitExceededException(String limitExceeded) {
        super(limitExceeded);
    }
}
