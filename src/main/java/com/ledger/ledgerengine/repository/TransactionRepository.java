package com.ledger.ledgerengine.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.ledger.ledgerengine.domain.Transaction;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Transaction> rowMapper = (rs, rowNum) -> new Transaction(
            UUID.fromString(rs.getString("transaction_id")),
            rs.getString("idempotency_key"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getString("error_reason"));

    public void save(Transaction transaction) {
        jdbcTemplate.update(
                "INSERT INTO transaction (transaction_id, idempotency_key, status, created_at, error_reason) VALUES (?, ?, ?, ?, ?)",
                transaction.transactionId(),
                transaction.idempotencyKey(),
                transaction.status(),
                java.sql.Timestamp.from(transaction.createdAt()),
                transaction.errorReason());
    }

    public void updateStatus(UUID transactionId, String status, String errorReason) {
        jdbcTemplate.update(
                "UPDATE transaction SET status = ?, error_reason = ? WHERE transaction_id = ?",
                status, errorReason, transactionId);
    }

    public Optional<Transaction> findById(UUID transactionId) {
        if (rowMapper != null) {
            return jdbcTemplate.query("SELECT * FROM transaction WHERE transaction_id = ?", rowMapper, transactionId)
                    .stream().findFirst();
        }
        return Optional.empty();
    }

    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        if (rowMapper != null) {
            return jdbcTemplate.query("SELECT * FROM transaction WHERE idempotency_key = ?", rowMapper, idempotencyKey)
                    .stream().findFirst();
        }
        return Optional.empty();
    }
}
