package com.banking.backend.util;

import java.util.UUID;

public class TransactionIdGenerator {
    public static String generate(){
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }
}
