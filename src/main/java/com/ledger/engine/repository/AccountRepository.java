package com.ledger.engine.repository;

import com.ledger.engine.domain.Account;
import com.ledger.engine.domain.AccountStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Account> ROW_MAPPER = (rs, rowNum) -> new Account(
            UUID.fromString(rs.getString("account_id")),
            AccountStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime());

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Account save(Account account) {
        jdbcTemplate.update(
                "INSERT INTO account (account_id, status) VALUES (?, ?)",
                account.getAccountId(), account.getStatus().name());
        return account;
    }

    public Optional<Account> findById(UUID accountId) {
        List<Account> results = jdbcTemplate.query(
                "SELECT account_id, status, created_at FROM account WHERE account_id = ?",
                ROW_MAPPER, accountId);
        return results.stream().findFirst();
    }

    public List<Account> findAll() {
        return jdbcTemplate.query(
                "SELECT account_id, status, created_at FROM account ORDER BY created_at DESC",
                ROW_MAPPER);
    }

    public Optional<Account> lockForUpdate(UUID accountId) {
        List<Account> results = jdbcTemplate.query(
                "SELECT account_id, status, created_at FROM account WHERE account_id = ? FOR UPDATE",
                ROW_MAPPER, accountId);
        return results.stream().findFirst();
    }
}
