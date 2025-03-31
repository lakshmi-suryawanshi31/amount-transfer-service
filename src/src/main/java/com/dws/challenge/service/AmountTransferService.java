package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.util.AccountLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Service
public class AmountTransferService {
    private static final Logger logger = LoggerFactory.getLogger(AmountTransferService.class);

    private final AccountsRepositoryInMemory accountsRepositoryInMemory;
    private final NotificationService notificationService;
    private final AmountTransferValidationService amountTransferValidationService;
    private final AmountTransferExecutionService amountTransferExecutionService;

    @Autowired
    public AmountTransferService(AccountsRepositoryInMemory accountsRepositoryInMemory,
                                 NotificationService notificationService,
                                 AmountTransferValidationService amountTransferValidationService,
                                 AmountTransferExecutionService amountTransferExecutionService) {
        this.accountsRepositoryInMemory = accountsRepositoryInMemory;
        this.notificationService = notificationService;
        this.amountTransferValidationService = amountTransferValidationService;
        this.amountTransferExecutionService = amountTransferExecutionService;
    }

    public String transferAmount(String accountFromId, String accountToId, BigDecimal amount) {
        try {
            // Validate Inputs
            amountTransferValidationService.validateTransferAmount(amount);

            Account accountFrom = accountsRepositoryInMemory.getAccount(accountFromId);
            Account accountTo = accountsRepositoryInMemory.getAccount(accountToId);

            // Add null check for accounts
            amountTransferValidationService.validateAccounts(accountFrom, accountTo);
            // Validate sufficient funds
            amountTransferValidationService.validateSufficientFunds(accountFrom, amount);

            // Acquire Locks Safely
            if (!acquireLocks(accountFrom, accountTo)) {
                return "Unable to acquire locks. Please try again later.";
            }

            try {
                logger.info("Attempting to transfer amount {} from account {} to account {}", amount, accountFromId, accountToId);
                // Execute Transfer
                amountTransferExecutionService.executeTransfer(accountFrom, accountTo, amount);

                // Notify Users
                sendNotifications(accountFrom, accountTo, amount);
                return "Transfer completed successfully.";
            } finally {
                // Release Locks
                releaseLocks(accountFrom, accountTo);
            }
        } catch (AmountTransferException e) {
            logger.error("Transfer failed: {}", e.getMessage(), e);
            return "Transfer failed: " + e.getMessage();
        } catch (Exception e) {
            logger.error("Unexpected error during transfer: {}", e.getMessage(), e);
            return "Unexpected error during transfer: " + e.getMessage();
        }
    }

    public boolean acquireLocks(Account accountFrom, Account accountTo) {
        // Ensure consistent locking order to prevent deadlocks
        Account firstAccount = accountFrom.getAccountId().compareTo(accountTo.getAccountId()) < 0 ? accountFrom : accountTo;
        Account secondAccount = accountFrom == firstAccount ? accountTo : accountFrom;

        Lock lock1 = AccountLockManager.getLock(firstAccount.getAccountId());
        Lock lock2 = AccountLockManager.getLock(secondAccount.getAccountId());

        try {
            boolean lock1Acquired = lock1.tryLock(5, TimeUnit.SECONDS);
            boolean lock2Acquired = lock2.tryLock(5, TimeUnit.SECONDS);

            if (!lock1Acquired || !lock2Acquired) {
                if (lock1Acquired) lock1.unlock();
                if (lock2Acquired) lock2.unlock();
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void releaseLocks(Account accountFrom, Account accountTo) {
        Lock lock1 = AccountLockManager.getLock(accountFrom.getAccountId());
        Lock lock2 = AccountLockManager.getLock(accountTo.getAccountId());

        if (lock1 != null) {
            lock1.unlock();
        }
        if (lock2 != null) {
            lock2.unlock();
        }
    }

    private void sendNotifications(Account accountFrom, Account accountTo, BigDecimal amount) {
        notificationService.notifyAboutTransfer(accountFrom, "Amount " + amount + " transferred to " + accountTo.getAccountId());
        logger.info("Notified account {} about transfer of amount {} to account {}", accountFrom.getAccountId(), amount, accountTo.getAccountId());
        notificationService.notifyAboutTransfer(accountTo, "Amount " + amount + " received from " + accountFrom.getAccountId());
        logger.info("Notified account {} about receipt of amount {} from account {}", accountTo.getAccountId(), amount, accountFrom.getAccountId());
    }
}