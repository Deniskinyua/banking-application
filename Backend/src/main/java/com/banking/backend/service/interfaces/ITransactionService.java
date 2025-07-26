package com.banking.backend.service;

import com.banking.backend.dto.TransactionRequestDTO;

public interface ITransactionService {

    void transferFunds(TransactionRequestDTO request);
}
