package com.ledger.ledgerengine.domain;

import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID transactionId,
        String idempotencyKey,
        String status,
        Instant createdAt,
        String errorReason) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}
