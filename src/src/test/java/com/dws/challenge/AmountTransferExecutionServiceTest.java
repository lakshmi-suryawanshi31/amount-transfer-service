package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.repository.AccountsRepositoryInMemory;
import com.dws.challenge.service.AmountTransferExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AmountTransferExecutionServiceTest {

    @Mock
    private AccountsRepositoryInMemory accountsRepositoryInMemory;

    @InjectMocks
    private AmountTransferExecutionService amountTransferExecutionService;

    private Account accountFrom;
    private Account accountTo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountFrom = new Account("1", new BigDecimal("1000"));
        accountTo = new Account("2", new BigDecimal("500"));
    }
    @Test
    void transferAmountSuccessfully() {
        when(accountsRepositoryInMemory.getAccount("1")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("2")).thenReturn(accountTo);

        amountTransferExecutionService.executeTransfer(accountFrom, accountTo, new BigDecimal("200"));

        assertEquals(new BigDecimal("800"), accountFrom.getBalance());
        assertEquals(new BigDecimal("700"), accountTo.getBalance());
    }
    @Test
    void transferAmountInsufficientFunds() {
        when(accountsRepositoryInMemory.getAccount("1")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("2")).thenReturn(accountTo);

        assertThrows(AmountTransferException.class, () ->
                amountTransferExecutionService.executeTransfer(accountFrom, accountTo, new BigDecimal("2000"))
        );
    }
    @Test
    void transferAmountZeroAmount() {
        when(accountsRepositoryInMemory.getAccount("1")).thenReturn(accountFrom);
        when(accountsRepositoryInMemory.getAccount("2")).thenReturn(accountTo);

        amountTransferExecutionService.executeTransfer(accountFrom, accountTo, BigDecimal.ZERO);

        assertEquals(new BigDecimal("1000"), accountFrom.getBalance());
        assertEquals(new BigDecimal("500"), accountTo.getBalance());
    }

}