package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AmountTransferServiceTest {

    @Mock
    private AccountsRepositoryInMemory repository;

    @Mock
    private AmountTransferValidationService validationService;

    @Mock
    private AmountTransferExecutionService executionService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AmountTransferService transferService;

    public AmountTransferServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void transferAmount_SuccessfulTransfer() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(repository.getAccount("123")).thenReturn(accountFrom);
        when(repository.getAccount("456")).thenReturn(accountTo);

        doNothing().when(validationService).validateTransferAmount(BigDecimal.valueOf(200));
        doNothing().when(validationService).validateAccounts(accountFrom, accountTo);
        doNothing().when(validationService).validateSufficientFunds(accountFrom, BigDecimal.valueOf(200));
        doNothing().when(executionService).executeTransfer(accountFrom, accountTo, BigDecimal.valueOf(200));

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Transfer completed successfully.", result);

        verify(notificationService, times(2)).notifyAboutTransfer(any(), any());
    }

    @Test
    public void transferAmount_InsufficientFunds() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(100));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(repository.getAccount("123")).thenReturn(accountFrom);
        when(repository.getAccount("456")).thenReturn(accountTo);

        doThrow(new AmountTransferException("Insufficient funds in account 123"))
                .when(validationService).validateSufficientFunds(accountFrom, BigDecimal.valueOf(200));

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Transfer failed: Insufficient funds in account 123", result);
    }

    @Test
    public void transferAmount_InvalidAccounts() {
        when(repository.getAccount("123")).thenReturn(null);
        when(repository.getAccount("456")).thenReturn(null);

        doThrow(new AmountTransferException("One or both accounts are invalid."))
                .when(validationService).validateAccounts(null, null);

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Transfer failed: One or both accounts are invalid.", result);
    }

    @Test
    public void transferAmount_UnableToAcquireLocks() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(repository.getAccount("123")).thenReturn(accountFrom);
        when(repository.getAccount("456")).thenReturn(accountTo);

        doNothing().when(validationService).validateTransferAmount(BigDecimal.valueOf(200));
        doNothing().when(validationService).validateAccounts(accountFrom, accountTo);
        doNothing().when(validationService).validateSufficientFunds(accountFrom, BigDecimal.valueOf(200));

        AmountTransferService spyTransferService = spy(transferService);
        doReturn(false).when(spyTransferService).acquireLocks(accountFrom, accountTo);

        String result = spyTransferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Unable to acquire locks. Please try again later.", result);
    }
    @Test
    public void transferAmount_UnexpectedError() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(repository.getAccount("123")).thenReturn(accountFrom);
        when(repository.getAccount("456")).thenReturn(accountTo);

        doNothing().when(validationService).validateTransferAmount(BigDecimal.valueOf(200));
        doNothing().when(validationService).validateAccounts(accountFrom, accountTo);
        doNothing().when(validationService).validateSufficientFunds(accountFrom, BigDecimal.valueOf(200));

        doThrow(new RuntimeException("Unexpected error")).when(executionService).executeTransfer(accountFrom, accountTo, BigDecimal.valueOf(200));

        String result = transferService.transferAmount("123", "456", BigDecimal.valueOf(200));
        assertEquals("Unexpected error during transfer: Unexpected error", result);
    }
}
