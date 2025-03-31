package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AmountTransferValidationService {

    public void validateTransferAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AmountTransferException("Transfer amount must be greater than zero.");
        }
    }

    public void validateAccounts(Account accountFrom, Account accountTo) {
        // Add null check for accounts
        if (accountFrom == null || accountTo == null) {
            throw new AmountTransferException("One or both accounts are invalid.");
        }
    }

    public void validateSufficientFunds(Account accountFrom, BigDecimal amount) {
        if (accountFrom == null) {
            throw new AmountTransferException("Source account is invalid.");
        }
        if (amount == null) {
            throw new AmountTransferException("Transfer amount is invalid.");
        }
        if (accountFrom.getBalance().compareTo(amount) < 0) {
            throw new AmountTransferException("Insufficient funds in account " + accountFrom.getAccountId());
        }
    }
}