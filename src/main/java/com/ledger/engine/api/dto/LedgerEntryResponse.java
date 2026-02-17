package com.ledger.engine.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntryResponse {

    private UUID ledgerEntryId;
    private UUID transactionId;
    private long amount;
    private LocalDateTime createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(UUID ledgerEntryId, UUID transactionId,
            long amount, LocalDateTime createdAt) {
        this.ledgerEntryId = ledgerEntryId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public void setLedgerEntryId(UUID ledgerEntryId) {
        this.ledgerEntryId = ledgerEntryId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
