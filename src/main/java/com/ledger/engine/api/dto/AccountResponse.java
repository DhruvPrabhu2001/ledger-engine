package com.ledger.engine.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountResponse {

    private UUID accountId;
    private String status;
    private LocalDateTime createdAt;

    public AccountResponse() {
    }

    public AccountResponse(UUID accountId, String status, LocalDateTime createdAt) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
