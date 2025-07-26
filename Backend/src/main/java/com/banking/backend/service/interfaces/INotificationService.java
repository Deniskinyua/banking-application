package com.banking.backend.service.interfaces;

import com.banking.backend.dto.TransactionNotification;
import com.banking.backend.model.Account;

import java.math.BigDecimal;

public interface INotificationService {

    void sendTransferNotifications(String transactionId, Account sender, Account recipient, BigDecimal amount);

}
