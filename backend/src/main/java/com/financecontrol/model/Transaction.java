package com.financecontrol.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    // Ejemplo: GASTRONOMICA MAN HU
    private String merchant;

    @Column(nullable = false)
    // Ejemplo: Consumo Tarjeta de Débito, Transferencia
    private String operationType;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    // Ejemplo: ****8614
    private String cardNumber;

    // Ejemplo: 166138
    private String operationNumber;

    @Enumerated(EnumType.STRING)
    private TransactionSource source;

    @Enumerated(EnumType.STRING)
    // null equivale a EXPENSE para registros previos a este campo
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private Category category;

    // Para evitar duplicados en la importación de Gmail
    private String gmailMessageId;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * onCreate asigna la fecha de creación antes de persistir la entidad.
     * @return void.
     */
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TransactionSource {
        BCP_EMAIL, MANUAL
    }

    public enum TransactionType {
        INCOME, EXPENSE
    }

    public enum Category {
        ALIMENTACION,
        TRANSPORTE,
        ENTRETENIMIENTO,
        SALUD,
        EDUCACION,
        SERVICIOS,
        COMPRAS,
        TRANSFERENCIA,
        OTROS,
        SALARIO,
        FREELANCE
    }
}
