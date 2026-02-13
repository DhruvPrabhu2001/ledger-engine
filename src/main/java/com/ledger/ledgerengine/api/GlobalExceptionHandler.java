package com.ledger.ledgerengine.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ledger.ledgerengine.exception.DuplicateTransactionException;
import com.ledger.ledgerengine.exception.InsufficientFundsException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientFunds(InsufficientFundsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INSUFFICIENT_FUNDS", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateTransactionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "DUPLICATE_TRANSACTION",
                        "message", e.getMessage(),
                        "existingTransactionId", e.getExistingTransaction().transactionId().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        e.printStackTrace(); // Log stack trace
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_ERROR", "message", "An unexpected error occurred"));
    }
}
