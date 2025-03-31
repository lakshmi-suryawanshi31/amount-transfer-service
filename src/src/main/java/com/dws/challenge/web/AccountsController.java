package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.AmountTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  private final AmountTransferService amountTransferService;

  @Autowired
  public AccountsController(AccountsService accountsService, AmountTransferService amountTransferService) {
    this.accountsService = accountsService;
    this.amountTransferService = amountTransferService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
      this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  @PostMapping("/amount-transfer")
  public ResponseEntity<String> transferAmount(@RequestBody TransferRequest transferRequest) {
      log.info("Transferring amount from {} to {}", transferRequest.getAccountFrom(), transferRequest.getAccountTo());
      String result = amountTransferService.transferAmount(
              transferRequest.getAccountFrom(),
              transferRequest.getAccountTo(),
              transferRequest.getAmount());
      return ResponseEntity.ok(result);

  }
}
