package com.financecontrol.dto;

import com.financecontrol.model.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class SummaryResponse {
    private BigDecimal totalSpent;
    private int transactionCount;
    private BigDecimal totalIncome;
    private int incomeCount;
    private Map<Transaction.Category, BigDecimal> byCategory;
    // Ejemplo: "2026-04" o "2026-04-01/2026-04-07"
    private String period;
}
