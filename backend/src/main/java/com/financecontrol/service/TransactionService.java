package com.financecontrol.service;

import com.financecontrol.dto.SummaryResponse;
import com.financecontrol.dto.TransactionRequest;
import com.financecontrol.model.Transaction;
import com.financecontrol.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;

    /**
     * findAll retorna todas las transacciones ordenadas por fecha descendente.
     * @return lista completa de transacciones.
     */
    public List<Transaction> findAll() {
        return repository.findAllByOrderByTransactionDateDesc();
    }

    /**
     * findByPeriod retorna las transacciones dentro de un rango de fechas.
     * @param from fecha/hora de inicio del rango (inclusive).
     * @param to   fecha/hora de fin del rango (inclusive).
     * @return lista de transacciones en el período, ordenadas por fecha descendente.
     */
    public List<Transaction> findByPeriod(LocalDateTime from, LocalDateTime to) {
        return repository.findByTransactionDateBetweenOrderByTransactionDateDesc(from, to);
    }

    /**
     * save persiste una transacción en la base de datos.
     * @param transaction entidad a guardar.
     * @return la transacción guardada con su ID asignado.
     */
    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    /**
     * createManual crea y persiste una transacción ingresada manualmente por el usuario.
     * @param req datos de la transacción provenientes del request.
     * @return la transacción creada.
     */
    public Transaction createManual(TransactionRequest req) {
        Transaction tx = Transaction.builder()
                .amount(req.getAmount())
                .merchant(req.getMerchant())
                .operationType(req.getOperationType() != null ? req.getOperationType() : "Manual")
                .transactionDate(req.getTransactionDate())
                .category(req.getCategory() != null ? req.getCategory() : Transaction.Category.OTROS)
                .notes(req.getNotes())
                .source(Transaction.TransactionSource.MANUAL)
                .type(req.getType() != null ? req.getType() : Transaction.TransactionType.EXPENSE)
                .build();
        return repository.save(tx);
    }

    /**
     * update modifica los campos de una transacción existente.
     * @param id  identificador de la transacción a actualizar.
     * @param req nuevos valores para los campos de la transacción.
     * @return la transacción actualizada.
     */
    public Transaction update(Long id, TransactionRequest req) {
        Transaction tx = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transacción no encontrada: " + id));
        tx.setAmount(req.getAmount());
        tx.setMerchant(req.getMerchant());
        tx.setTransactionDate(req.getTransactionDate());
        if (req.getCategory() != null) tx.setCategory(req.getCategory());
        if (req.getNotes() != null) tx.setNotes(req.getNotes());
        if (req.getType() != null) tx.setType(req.getType());
        return repository.save(tx);
    }

    /**
     * delete elimina una transacción por su ID.
     * @param id identificador de la transacción a eliminar.
     * @return void.
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * getSummary calcula el resumen de gastos e ingresos agrupado por categoría para un mes.
     * @param month mes a resumir en formato YearMonth.
     * @return resumen con totales por categoría, total gastado e ingresado.
     */
    public SummaryResponse getSummary(YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> rows = repository.sumByCategory(from, to);
        Map<Transaction.Category, BigDecimal> byCategory = new EnumMap<>(Transaction.Category.class);
        for (Object[] row : rows) {
            byCategory.put((Transaction.Category) row[0], (BigDecimal) row[1]);
        }

        BigDecimal total = repository.totalBetween(from, to).orElse(BigDecimal.ZERO);
        BigDecimal totalIncome = repository.totalIncomeBetween(from, to).orElse(BigDecimal.ZERO);
        int incomeCount = repository.countIncomeBetween(from, to);
        List<Transaction> txs = findByPeriod(from, to);
        long expenseCount = txs.stream()
                .filter(t -> t.getType() == null || t.getType() == Transaction.TransactionType.EXPENSE)
                .count();

        return SummaryResponse.builder()
                .totalSpent(total)
                .transactionCount((int) expenseCount)
                .totalIncome(totalIncome)
                .incomeCount(incomeCount)
                .byCategory(byCategory)
                .period(month.toString())
                .build();
    }

    /**
     * existsByGmailId verifica si ya existe una transacción importada con el ID de email dado.
     * @param gmailMessageId identificador del mensaje de Gmail.
     * @return true si la transacción ya fue importada; false en caso contrario.
     */
    public boolean existsByGmailId(String gmailMessageId) {
        return repository.findByGmailMessageId(gmailMessageId).isPresent();
    }
}
