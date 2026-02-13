package com.ledger.ledgerengine.dto;

import java.util.List;
import java.util.UUID;

public record TransferRequest(
                String idempotencyKey,
                List<TransferEntry> entries) {
        public record TransferEntry(
                        UUID accountId,
                        long amount) {
        }
}
