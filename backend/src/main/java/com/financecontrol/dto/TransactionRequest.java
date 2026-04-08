package com.financecontrol.dto;

import com.financecontrol.model.Transaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private String merchant;

    private String operationType;

    @NotNull
    private LocalDateTime transactionDate;

    private Transaction.Category category;

    private Transaction.TransactionType type;

    private String notes;
}
