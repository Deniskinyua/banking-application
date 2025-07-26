package com.banking.backend.service.interfaces;

import com.banking.backend.dto.TransactionRequestDTO;

public interface ITransactionService {

    void transferFunds(TransactionRequestDTO request);
}
