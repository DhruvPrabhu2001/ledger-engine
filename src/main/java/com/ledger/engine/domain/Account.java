package com.ledger.engine.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Account {

    private UUID accountId;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public Account() {
    }

    public Account(UUID accountId, AccountStatus status, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
