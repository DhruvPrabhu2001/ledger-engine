package com.ledger.ledgerengine.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.ledger.ledgerengine.domain.Account;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Account> rowMapper = (rs, rowNum) -> new Account(
            UUID.fromString(rs.getString("account_id")),
            rs.getString("currency"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant());

    public void save(Account account) {
        jdbcTemplate.update(
                "INSERT INTO account (account_id, currency, status, created_at) VALUES (?, ?, ?, ?)",
                account.accountId(), account.currency(), account.status(),
                java.sql.Timestamp.from(account.createdAt()));
    }

    public Optional<Account> findById(UUID accountId) {
        if (rowMapper != null) {
            return jdbcTemplate.query("SELECT * FROM account WHERE account_id = ?", rowMapper, accountId)
                    .stream().findFirst();
        }
        return Optional.empty();
    }

    // Pessimistic Lock
    public Optional<Account> findByIdForUpdate(UUID accountId) {
        if (rowMapper != null) {
            return jdbcTemplate.query("SELECT * FROM account WHERE account_id = ? FOR UPDATE", rowMapper, accountId)
                    .stream().findFirst();
        }
        return Optional.empty();

    }
}
