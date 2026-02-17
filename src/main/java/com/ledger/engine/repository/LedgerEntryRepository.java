package com.ledger.engine.repository;

import com.ledger.engine.domain.LedgerEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class LedgerEntryRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LedgerEntry> ROW_MAPPER = (rs, rowNum) -> new LedgerEntry(
            UUID.fromString(rs.getString("ledger_entry_id")),
            UUID.fromString(rs.getString("transaction_id")),
            UUID.fromString(rs.getString("account_id")),
            rs.getLong("amount"),
            rs.getTimestamp("created_at").toLocalDateTime());

    public LedgerEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAll(List<LedgerEntry> entries) {
        for (LedgerEntry entry : entries) {
            jdbcTemplate.update(
                    "INSERT INTO ledger_entry (ledger_entry_id, transaction_id, account_id, amount) VALUES (?, ?, ?, ?)",
                    entry.getLedgerEntryId(),
                    entry.getTransactionId(),
                    entry.getAccountId(),
                    entry.getAmount());
        }
    }

    public long deriveBalance(UUID accountId) {
        Long balance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry WHERE account_id = ?",
                Long.class, accountId);
        return balance != null ? balance : 0L;
    }

    public List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return jdbcTemplate.query(
                "SELECT ledger_entry_id, transaction_id, account_id, amount, created_at " +
                        "FROM ledger_entry WHERE transaction_id = ? ORDER BY created_at",
                ROW_MAPPER, transactionId);
    }

    public List<LedgerEntry> findByAccountId(UUID accountId) {
        return jdbcTemplate.query(
                "SELECT ledger_entry_id, transaction_id, account_id, amount, created_at " +
                        "FROM ledger_entry WHERE account_id = ? ORDER BY created_at DESC",
                ROW_MAPPER, accountId);
    }

    public long globalLedgerSum() {
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ledger_entry",
                Long.class);
        return sum != null ? sum : 0L;
    }
}
