package com.ledger.engine.api;

import com.ledger.engine.api.dto.*;
import com.ledger.engine.domain.Transaction;
import com.ledger.engine.service.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final LedgerService ledgerService;

    public TransactionController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@RequestBody DepositRequest request) {
        validateDepositRequest(request);
        Transaction tx = ledgerService.deposit(
                request.getAccountId(),
                request.getAmount(),
                request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tx));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@RequestBody WithdrawRequest request) {
        validateWithdrawRequest(request);
        Transaction tx = ledgerService.withdraw(
                request.getAccountId(),
                request.getAmount(),
                request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tx));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransferRequest request) {
        validateTransferRequest(request);
        Transaction tx = ledgerService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tx));
    }

    private void validateDepositRequest(DepositRequest request) {
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
    }

    private void validateWithdrawRequest(WithdrawRequest request) {
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.getFromAccountId() == null) {
            throw new IllegalArgumentException("fromAccountId is required");
        }
        if (request.getToAccountId() == null) {
            throw new IllegalArgumentException("toAccountId is required");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getTransactionId(),
                tx.getIdempotencyKey(),
                tx.getStatus().name(),
                tx.getCreatedAt());
    }
}
