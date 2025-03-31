package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.AmountTransferExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class TransferExecutionServiceTest {

    @Mock
    private AccountsRepositoryInMemory repository;

    @InjectMocks
    private AmountTransferExecutionService executionService;

    public TransferExecutionServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testExecuteTransferSuccess() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(repository.getAccount("123")).thenReturn(accountFrom);
        when(repository.getAccount("456")).thenReturn(accountTo);

        assertDoesNotThrow(() -> executionService.executeTransfer(accountFrom, accountTo, BigDecimal.valueOf(200)));

        assertEquals(BigDecimal.valueOf(300), accountFrom.getBalance());
        assertEquals(BigDecimal.valueOf(500), accountTo.getBalance());
    }

    @Test
    public void testExecuteTransferInsufficientFunds() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(100));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));

        when(repository.getAccount("123")).thenReturn(accountFrom);
        when(repository.getAccount("456")).thenReturn(accountTo);

        Exception exception = assertThrows(AmountTransferException.class,
            () -> executionService.executeTransfer(accountFrom, accountTo, BigDecimal.valueOf(200)));
        assertEquals("Insufficient funds in account 123", exception.getMessage());
    }
}
