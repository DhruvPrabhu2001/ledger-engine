package com.ledger.engine.exception;

public class AccountClosedException extends RuntimeException {

    public AccountClosedException(String message) {
        super(message);
    }
}
