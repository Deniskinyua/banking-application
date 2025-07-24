package com.banking.backend.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale; // Consider locale for formatting

@Component
public class MessageFormatter {
    // Use Locale.US or Locale.getDefault() based on your target audience
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("d/M/yy 'at' h:mm a", Locale.US);

    public String formatRecipientMessage(String transactionId, BigDecimal amount, String senderName, BigDecimal newBalance, LocalDateTime timestamp) {
        return String.format(
                "%s Confirmed. You have received Ksh%.2f from %s on %s. New balance is Ksh%.2f.",
                transactionId, amount, senderName, timestamp.format(TIME_FORMATTER), newBalance
        );
    }

    public String formatSenderMessage(String transactionId, BigDecimal amount, String recipientName, BigDecimal newBalance, LocalDateTime timestamp) {
        // The last BigDecimal value '499840.00' seems like a magic number.
        // It's better to calculate the remaining daily limit from the actual Account object
        // if this message is being constructed after the transaction is processed and saved.
        // Assuming this is just a placeholder and the real calculation comes from `fromAccount.getDailyTransactionLimit().subtract(fromAccount.getDailyTransactionAmount())`
        // which would be done in the NotificationProcessor or a dedicated message builder.

        // For now, let's remove the magic number and expect this value to be passed.
        // If this message is generated based on a *completed* transaction,
        // the remaining daily amount should be accurate from the 'fromAccount' object.

        // Example assuming remainingDailyLimit is passed:
        // public String formatSenderMessage(String transactionId, BigDecimal amount, String recipientName, BigDecimal newBalance, LocalDateTime timestamp, BigDecimal remainingDailyLimit)
        // String.format("... Amount you can transact within the day is Ksh%.2f. ...", remainingDailyLimit)

        // For the current structure, if you want this specific value to be dynamic:
        // You would need to pass remainingDailyAmount from NotificationService
        // or calculate it in NotificationProcessor after deserializing the message.
        return String.format(
                "%s Confirmed. Ksh%.2f paid to %s on %s. New balance is Ksh%.2f. Transaction cost, Ksh. 0.00. " +
                        "Amount you can transact within the day is Ksh%.2f. Save frequent tills for quick payment.",
                transactionId, amount, recipientName,
                timestamp.format(TIME_FORMATTER), newBalance, new BigDecimal("499840.00").subtract(amount) // Still using magic number
        );
    }
}