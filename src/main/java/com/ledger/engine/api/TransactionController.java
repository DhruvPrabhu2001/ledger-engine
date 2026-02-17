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
        Transaction tx = ledgerService.deposit(
                request.getAccountId(),
                request.getAmount(),
                request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tx));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@RequestBody WithdrawRequest request) {
        Transaction tx = ledgerService.withdraw(
                request.getAccountId(),
                request.getAmount(),
                request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tx));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransferRequest request) {
        Transaction tx = ledgerService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tx));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getTransactionId(),
                tx.getIdempotencyKey(),
                tx.getStatus().name(),
                tx.getCreatedAt());
    }
}
