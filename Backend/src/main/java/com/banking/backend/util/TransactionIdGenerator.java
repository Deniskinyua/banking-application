package com.banking.backend.util;

import java.util.UUID;

public class TransactionIdGenerator {
    // Make utility methods static and private constructor to prevent instantiation
    private TransactionIdGenerator() {
        // Private constructor to prevent instantiation
    }

    public static String generate(){
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}