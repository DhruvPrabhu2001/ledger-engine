package com.ledger.ledgerengine.domain;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
        UUID ledgerEntryId,
        UUID transactionId,
        UUID accountId,
        long amount,
        Instant createdAt) {
}
