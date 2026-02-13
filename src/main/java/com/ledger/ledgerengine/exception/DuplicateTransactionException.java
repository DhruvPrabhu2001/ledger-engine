package com.ledger.ledgerengine.exception;

import com.ledger.ledgerengine.domain.Transaction;

public class DuplicateTransactionException extends RuntimeException {
    private final Transaction existingTransaction;

    public DuplicateTransactionException(Transaction existingTransaction) {
        super("Duplicate transaction: " + existingTransaction.transactionId());
        this.existingTransaction = existingTransaction;
    }

    public Transaction getExistingTransaction() {
        return existingTransaction;
    }
}
