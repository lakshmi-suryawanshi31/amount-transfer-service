package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AmountTransferException;
import com.dws.challenge.service.AmountTransferValidationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class AmountTransferValidationServiceTest {

    private final AmountTransferValidationService validationService = new AmountTransferValidationService();

    @Test
    public void testValidateValidTransferAmount() {
        assertDoesNotThrow(() -> validationService.validateTransferAmount(BigDecimal.valueOf(100)));
    }

    @Test
    public void testValidateNegativeTransferAmount() {
        Exception exception = assertThrows(AmountTransferException.class,
            () -> validationService.validateTransferAmount(BigDecimal.valueOf(-100)));
        assertEquals("Transfer amount must be greater than zero.", exception.getMessage());
    }

    @Test
    public void testValidateValidAccounts() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Account accountTo = new Account("456", BigDecimal.valueOf(300));
        assertDoesNotThrow(() -> validationService.validateAccounts(accountFrom, accountTo));
    }

    @Test
    public void testValidateNullAccount() {
        Account accountFrom = new Account("123", BigDecimal.valueOf(500));
        Exception exception = assertThrows(AmountTransferException.class,
            () -> validationService.validateAccounts(accountFrom, null));
        assertEquals("One or both accounts are invalid.", exception.getMessage());
    }

    @Test
    public void testValidateInsufficientFunds() {
        Account account = new Account("123", BigDecimal.valueOf(100));
        Exception exception = assertThrows(AmountTransferException.class,
            () -> validationService.validateSufficientFunds(account, BigDecimal.valueOf(200)));
        assertEquals("Insufficient funds in account 123", exception.getMessage());
    }
}
