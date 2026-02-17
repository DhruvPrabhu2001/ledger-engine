package com.ledger.engine.exception;

import java.util.UUID;

public class DuplicateRequestException extends RuntimeException {

    private final UUID existingTransactionId;

    public DuplicateRequestException(String message, UUID existingTransactionId) {
        super(message);
        this.existingTransactionId = existingTransactionId;
    }

    public UUID getExistingTransactionId() {
        return existingTransactionId;
    }
}
