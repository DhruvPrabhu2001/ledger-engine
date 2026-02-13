package com.ledger.ledgerengine.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ledger.ledgerengine.dto.TransactionResult;
import com.ledger.ledgerengine.dto.TransferRequest;
import com.ledger.ledgerengine.service.LedgerService;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final LedgerService ledgerService;

    public TransferController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<TransactionResult> transfer(@RequestBody TransferRequest request) {
        TransactionResult result = ledgerService.processTransaction(request);
        return ResponseEntity.ok(result);
    }
}
