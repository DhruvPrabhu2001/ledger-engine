package com.ledger.ledgerengine.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.ledger.ledgerengine.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

@Repository
public class LedgerRepository {

    private final JdbcTemplate jdbcTemplate;

    public LedgerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<LedgerEntry> rowMapper = (rs, rowNum) -> new LedgerEntry(
            UUID.fromString(rs.getString("ledger_entry_id")),
            UUID.fromString(rs.getString("transaction_id")),
            UUID.fromString(rs.getString("account_id")),
            rs.getLong("amount"),
            rs.getTimestamp("created_at").toInstant());

    public void save(LedgerEntry entry) {
        jdbcTemplate.update(
                "INSERT INTO ledger_entry (ledger_entry_id, transaction_id, account_id, amount, created_at) VALUES (?, ?, ?, ?, ?)",
                entry.ledgerEntryId(),
                entry.transactionId(),
                entry.accountId(),
                entry.amount(),
                java.sql.Timestamp.from(entry.createdAt()));
    }

    public List<LedgerEntry> findByTransactionId(UUID transactionId) {
        if (rowMapper != null) {
            return jdbcTemplate.query("SELECT * FROM ledger_entry WHERE transaction_id = ?", rowMapper, transactionId);
        }
        return null;
    }

    public Long getBalance(UUID accountId) {
        Long balance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry WHERE account_id = ?",
                Long.class,
                accountId);
        return balance != null ? balance : 0L;
    }

    public List<LedgerEntry> findByAccountId(UUID accountId) {
        if (rowMapper != null) {
            return jdbcTemplate.query("SELECT * FROM ledger_entry WHERE account_id = ? ORDER BY created_at DESC",
                    rowMapper,
                    accountId);
        }
        return null;
    }
}
