package com.ledger.engine.repository;

import com.ledger.engine.domain.Transaction;
import com.ledger.engine.domain.TransactionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Transaction> ROW_MAPPER = (rs, rowNum) -> new Transaction(
            UUID.fromString(rs.getString("transaction_id")),
            rs.getString("idempotency_key"),
            TransactionStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime());

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Transaction save(Transaction transaction) {
        jdbcTemplate.update(
                "INSERT INTO transaction (transaction_id, idempotency_key, status) VALUES (?, ?, ?)",
                transaction.getTransactionId(),
                transaction.getIdempotencyKey(),
                transaction.getStatus().name());
        return transaction;
    }

    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        List<Transaction> results = jdbcTemplate.query(
                "SELECT transaction_id, idempotency_key, status, created_at FROM transaction WHERE idempotency_key = ?",
                ROW_MAPPER, idempotencyKey);
        return results.stream().findFirst();
    }

    public Optional<Transaction> findById(UUID transactionId) {
        List<Transaction> results = jdbcTemplate.query(
                "SELECT transaction_id, idempotency_key, status, created_at FROM transaction WHERE transaction_id = ?",
                ROW_MAPPER, transactionId);
        return results.stream().findFirst();
    }

    public void updateStatus(UUID transactionId, TransactionStatus status) {
        jdbcTemplate.update(
                "UPDATE transaction SET status = ? WHERE transaction_id = ?",
                status.name(), transactionId);
    }
}
