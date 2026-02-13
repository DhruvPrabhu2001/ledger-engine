package com.ledger.ledgerengine.dto;

import java.time.Instant;
import java.util.UUID;

public record TransactionResult(
                UUID transactionId,
                String status,
                String errorReason,
                Instant timestamp) {
}
