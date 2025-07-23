package com.banking.backend.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MessageFormatter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("d/M/yy 'at' h:mm a");

    public String formatRecipientMessage(String transactionId, BigDecimal amount, String senderName, BigDecimal newBalance, LocalDateTime timestamp) {
        return String.format(
                "%s Confirmed. You have received Ksh%.2f from %s on %s. "+ "New balance is Ksh%.2f.",
                transactionId, amount, senderName, timestamp.format(TIME_FORMATTER), newBalance
        );
    }

    public String formatSenderMessage(String transactionId, BigDecimal amount, String recipientName, BigDecimal newBalance, LocalDateTime timestamp) {
        return String.format(
                "%s Confirmed. Ksh%.2f paid to %s on %s. New balance is Ksh%.2f." +
                        "Transaction cost, Ksh. 0.00. Amount you can transact within the day is Ksh%.2f. "+
                        "Save frequest tills for quick payment.", transactionId, amount, recipientName,
                timestamp.format(TIME_FORMATTER), newBalance, new BigDecimal("499840.00").subtract(amount)
        );
    }
}
