package com.ledger.ledgerengine.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.ledger.ledgerengine.domain.LedgerEntry;
import com.ledger.ledgerengine.domain.Transaction;
import com.ledger.ledgerengine.dto.TransactionResult;
import com.ledger.ledgerengine.dto.TransferRequest;
import com.ledger.ledgerengine.exception.DuplicateTransactionException;
import com.ledger.ledgerengine.exception.InsufficientFundsException;
import com.ledger.ledgerengine.repository.AccountRepository;
import com.ledger.ledgerengine.repository.LedgerRepository;
import com.ledger.ledgerengine.repository.TransactionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    public LedgerService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResult processTransaction(TransferRequest request) {
        // 1. Idempotency Check
        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingTx.isPresent()) {
            throw new DuplicateTransactionException(existingTx.get());
        }

        // 2. Validate Sum = 0
        long sum = request.entries().stream().mapToLong(TransferRequest.TransferEntry::amount).sum();
        if (sum != 0) {
            throw new IllegalArgumentException("Transaction entries must sum to 0");
        }

        // 3. Create Transaction Record (PENDING)
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                transactionId,
                request.idempotencyKey(),
                Transaction.STATUS_PENDING,
                Instant.now(),
                null);
        transactionRepository.save(transaction);

        try {
            // 4. Lock Accounts (Ordered to prevent deadlocks)
            List<UUID> sortedAccountIds = request.entries().stream()
                    .map(TransferRequest.TransferEntry::accountId)
                    .sorted(UUID::compareTo)
                    .distinct()
                    .toList();

            for (UUID accountId : sortedAccountIds) {
                accountRepository.findByIdForUpdate(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
            }

            // 5. Business Logic & Insert Entries
            for (TransferRequest.TransferEntry entry : request.entries()) {
                // Check Balance for Debits
                if (entry.amount() < 0) {
                    Long currentBalance = ledgerRepository.getBalance(entry.accountId());
                    if (currentBalance + entry.amount() < 0) {
                        throw new InsufficientFundsException("Insufficient funds for account: " + entry.accountId());
                    }
                }

                LedgerEntry ledgerEntry = new LedgerEntry(
                        UUID.randomUUID(),
                        transactionId,
                        entry.accountId(),
                        entry.amount(),
                        Instant.now());
                ledgerRepository.save(ledgerEntry);
            }

            // 6. Complete Transaction
            transactionRepository.updateStatus(transactionId, Transaction.STATUS_COMPLETED, null);

            return new TransactionResult(transactionId, Transaction.STATUS_COMPLETED, null, transaction.createdAt());

        } catch (Exception e) {
            // Mark Transaction FAILED
            transactionRepository.updateStatus(transactionId, Transaction.STATUS_FAILED, e.getMessage());
            throw e;
        }
    }
}
