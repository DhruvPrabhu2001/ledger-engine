package com.ledger.moneyengine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ledger.ledgerengine.domain.Account;
import com.ledger.ledgerengine.dto.TransactionResult;
import com.ledger.ledgerengine.dto.TransferRequest;
import com.ledger.ledgerengine.exception.DuplicateTransactionException;
import com.ledger.ledgerengine.exception.InsufficientFundsException;
import com.ledger.ledgerengine.service.AccountService;
import com.ledger.ledgerengine.service.LedgerService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LedgerInvariantTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void testZeroSumInvariant() {
        Account accA = accountService.createAccount("USD");
        Account accB = accountService.createAccount("USD");

        TransferRequest invalidRequest = new TransferRequest(
                UUID.randomUUID().toString(),
                List.of(
                        new TransferRequest.TransferEntry(accA.accountId(), -100),
                        new TransferRequest.TransferEntry(accB.accountId(), 200) // Sum = 100 != 0
                ));

        assertThrows(IllegalArgumentException.class, () -> ledgerService.processTransaction(invalidRequest));
    }

    @Test
    void testIdempotency() {
        Account treasury = accountService.createAccount("USD");
        Account accA = accountService.createAccount("USD");

        String idempotencyKey = UUID.randomUUID().toString();

        // 1. First successful transfer
        TransferRequest request = new TransferRequest(
                idempotencyKey,
                List.of(
                        new TransferRequest.TransferEntry(treasury.accountId(), -100),
                        new TransferRequest.TransferEntry(accA.accountId(), 100)));

        TransactionResult result1 = ledgerService.processTransaction(request);
        assertNotNull(result1.transactionId());
        assertEquals("COMPLETED", result1.status());

        // 2. Duplicate Request
        DuplicateTransactionException exception = assertThrows(DuplicateTransactionException.class, () -> {
            ledgerService.processTransaction(request);
        });

        assertEquals(result1.transactionId(), exception.getExistingTransaction().transactionId());
    }

    @Test
    void testInsufficientFunds() {
        Account accA = accountService.createAccount("USD"); // Balance 0
        Account accB = accountService.createAccount("USD");

        TransferRequest request = new TransferRequest(
                UUID.randomUUID().toString(),
                List.of(
                        new TransferRequest.TransferEntry(accA.accountId(), -100), // Debit 100 from 0
                        new TransferRequest.TransferEntry(accB.accountId(), 100)));

        assertThrows(InsufficientFundsException.class, () -> ledgerService.processTransaction(request));
    }
}
