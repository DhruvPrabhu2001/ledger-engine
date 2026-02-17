package com.ledger.engine.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionResponse {

    private UUID transactionId;
    private String idempotencyKey;
    private String status;
    private LocalDateTime createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(UUID transactionId, String idempotencyKey,
            String status, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
