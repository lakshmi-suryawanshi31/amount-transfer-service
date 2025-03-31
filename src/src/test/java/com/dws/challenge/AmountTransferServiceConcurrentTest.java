package com.dws.challenge;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.AmountTransferService;
import com.dws.challenge.service.NotificationService;
import com.dws.challenge.service.AmountTransferExecutionService;
import com.dws.challenge.service.AmountTransferValidationService;
import com.dws.challenge.util.AccountLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

public class AmountTransferServiceConcurrentTest {

    @InjectMocks
    private AmountTransferService transferService;

    @Mock
    private AccountsRepositoryInMemory accountsRepositoryInMemory;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AmountTransferValidationService amountTransferValidationService;

    @Mock
    private AmountTransferExecutionService amountTransferExecutionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        AccountLockManager.cleanUpLock(anyString());
    }
    @Test
    public void transferAmount_Successful() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(accountsRepositoryInMemory.getAccount("123")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("456")).thenReturn(accountTo);

        doNothing().when(amountTransferValidationService).validateTransferAmount(BigDecimal.valueOf(200));
        doNothing().when(amountTransferValidationService).validateAccounts(accountFrom, accountTo);
        doNothing().when(amountTransferValidationService).validateSufficientFunds(accountFrom, BigDecimal.valueOf(200));
        doNothing().when(amountTransferExecutionService).executeTransfer(accountFrom, accountTo, BigDecimal.valueOf(200));

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Transfer completed successfully.", result);

        verify(notificationService, times(2)).notifyAboutTransfer(any(), any());
    }

    @Test
    public void transferAmount_InsufficientFunds() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(100));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(accountsRepositoryInMemory.getAccount("123")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("456")).thenReturn(accountTo);

        doThrow(new AmountTransferException("Insufficient funds in account 123"))
                .when(amountTransferValidationService).validateSufficientFunds(accountFrom, BigDecimal.valueOf(200));

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Transfer failed: Insufficient funds in account 123", result);
    }

    @Test
    public void transferAmount_InvalidAccounts() {
        when(accountsRepositoryInMemory.getAccount("123")).thenReturn(null);
        when(accountsRepositoryInMemory.getAccount("456")).thenReturn(null);

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Transfer failed: One or both accounts are invalid.", result);
    }

    @Test
    public void transferAmount_LockFailure() throws InterruptedException {
        Account accountFrom = new Account("123", BigDecimal.valueOf(1000));
        Account accountTo = new Account("456", BigDecimal.valueOf(500));

        when(accountsRepositoryInMemory.getAccount("123")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("456")).thenReturn(accountTo);

        try (MockedStatic<AccountLockManager> lockManager = mockStatic(AccountLockManager.class)) {
            Lock mockLock = mock(Lock.class);
            lockManager.when(() -> AccountLockManager.getLock(anyString())).thenReturn(mockLock);
            when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

            String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(100));
            assertEquals("Unable to acquire locks. Please try again later.", result);
        }
    }

    @Test
    public void transferAmount_UnexpectedError() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(accountsRepositoryInMemory.getAccount("123")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("456")).thenReturn(accountTo);

        doThrow(new RuntimeException("Unexpected error")).when(amountTransferExecutionService).executeTransfer(accountFrom, accountTo, BigDecimal.valueOf(200));

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Unexpected error during transfer: Unexpected error", result);
    }

}
