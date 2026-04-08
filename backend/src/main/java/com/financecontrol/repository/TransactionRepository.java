package com.financecontrol.repository;

import com.financecontrol.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByGmailMessageId(String gmailMessageId);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDateTime from, LocalDateTime to);

    List<Transaction> findAllByOrderByTransactionDateDesc();

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.transactionDate BETWEEN :from AND :to " +
           "AND (t.type = 'EXPENSE' OR t.type IS NULL) " +
           "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumByCategory(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.transactionDate BETWEEN :from AND :to " +
           "AND (t.type = 'EXPENSE' OR t.type IS NULL)")
    Optional<java.math.BigDecimal> totalBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.transactionDate BETWEEN :from AND :to AND t.type = 'INCOME'")
    Optional<java.math.BigDecimal> totalIncomeBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.transactionDate BETWEEN :from AND :to AND t.type = 'INCOME'")
    int countIncomeBetween(LocalDateTime from, LocalDateTime to);
}
