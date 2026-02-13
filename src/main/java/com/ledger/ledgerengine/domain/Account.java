package com.ledger.ledgerengine.domain;

import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID accountId,
        String currency,
        String status,
        Instant createdAt) {
}
