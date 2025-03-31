package com.dws.challenge.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotNull
    private String accountFrom;
    @NotNull
    private String accountTo;
    @NotNull
    private BigDecimal amount;
}
