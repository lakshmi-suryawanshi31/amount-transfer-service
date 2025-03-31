package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AmountTransferExecutionService {

    @Autowired
    private final AccountsRepositoryInMemory accountsRepositoryInMemory;

    public AmountTransferExecutionService(AccountsRepositoryInMemory accountsRepositoryInMemory) {
        this.accountsRepositoryInMemory = accountsRepositoryInMemory;
    }

    public void executeTransfer(Account accountFrom, Account accountTo, BigDecimal amount) {

        synchronized (this) {
            if (accountFrom.getBalance().compareTo(amount) < 0) {
                throw new AmountTransferException("Insufficient funds in account " + accountFrom.getAccountId());
            }

            // Perform Transaction
            accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
            accountTo.setBalance(accountTo.getBalance().add(amount));

            // Persist Updates
            accountsRepositoryInMemory.getAccounts().put(accountFrom.getAccountId(), accountFrom);
            accountsRepositoryInMemory.getAccounts().put(accountTo.getAccountId(), accountTo);
        }
    }
}