package com.ledger.engine.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private UUID transactionId;
    private String idempotencyKey;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(UUID transactionId, String idempotencyKey,
            TransactionStatus status, LocalDateTime createdAt) {
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

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
