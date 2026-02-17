package com.ledger.engine.service;

import com.ledger.engine.domain.LedgerEntry;
import com.ledger.engine.domain.Transaction;
import com.ledger.engine.domain.TransactionStatus;
import com.ledger.engine.exception.AccountNotFoundException;
import com.ledger.engine.exception.DuplicateRequestException;
import com.ledger.engine.exception.InsufficientFundsException;
import com.ledger.engine.repository.AccountRepository;
import com.ledger.engine.repository.LedgerEntryRepository;
import com.ledger.engine.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Transaction deposit(UUID accountId, long amount, String idempotencyKey) {
        validateAmount(amount);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate deposit request detected: idempotencyKey={}", idempotencyKey);
            throw new DuplicateRequestException(
                    "Request already processed: " + idempotencyKey,
                    existing.get().getTransactionId());
        }

        accountRepository.lockForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(txId, idempotencyKey,
                TransactionStatus.COMPLETED, LocalDateTime.now());
        transactionRepository.save(transaction);

        LedgerEntry credit = new LedgerEntry(UUID.randomUUID(), txId, accountId, amount, LocalDateTime.now());
        ledgerEntryRepository.saveAll(Collections.singletonList(credit));

        log.info("Deposit completed: txId={}, accountId={}, amount={}", txId, accountId, amount);
        return transaction;
    }

    @Transactional
    public Transaction withdraw(UUID accountId, long amount, String idempotencyKey) {
        validateAmount(amount);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate withdraw request detected: idempotencyKey={}", idempotencyKey);
            throw new DuplicateRequestException(
                    "Request already processed: " + idempotencyKey,
                    existing.get().getTransactionId());
        }

        accountRepository.lockForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        long balance = ledgerEntryRepository.deriveBalance(accountId);
        if (balance < amount) {
            log.warn("Insufficient funds: accountId={}, balance={}, requested={}", accountId, balance, amount);
            throw new InsufficientFundsException(
                    String.format("Insufficient funds: balance=%d, requested=%d", balance, amount));
        }

        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(txId, idempotencyKey,
                TransactionStatus.COMPLETED, LocalDateTime.now());
        transactionRepository.save(transaction);

        LedgerEntry debit = new LedgerEntry(UUID.randomUUID(), txId, accountId, -amount, LocalDateTime.now());
        ledgerEntryRepository.saveAll(Collections.singletonList(debit));

        log.info("Withdrawal completed: txId={}, accountId={}, amount={}", txId, accountId, amount);
        return transaction;
    }

    @Transactional
    public Transaction transfer(UUID fromAccountId, UUID toAccountId, long amount, String idempotencyKey) {
        validateAmount(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate transfer request detected: idempotencyKey={}", idempotencyKey);
            throw new DuplicateRequestException(
                    "Request already processed: " + idempotencyKey,
                    existing.get().getTransactionId());
        }

        List<UUID> sortedIds = Arrays.asList(fromAccountId, toAccountId);
        Collections.sort(sortedIds);

        for (UUID id : sortedIds) {
            accountRepository.lockForUpdate(id)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
        }

        long sourceBalance = ledgerEntryRepository.deriveBalance(fromAccountId);
        if (sourceBalance < amount) {
            log.warn("Insufficient funds for transfer: fromAccountId={}, balance={}, requested={}",
                    fromAccountId, sourceBalance, amount);
            throw new InsufficientFundsException(
                    String.format("Insufficient funds: balance=%d, requested=%d", sourceBalance, amount));
        }

        UUID txId = UUID.randomUUID();
        Transaction transaction = new Transaction(txId, idempotencyKey,
                TransactionStatus.COMPLETED, LocalDateTime.now());
        transactionRepository.save(transaction);

        LedgerEntry debit = new LedgerEntry(UUID.randomUUID(), txId, fromAccountId, -amount, LocalDateTime.now());
        LedgerEntry credit = new LedgerEntry(UUID.randomUUID(), txId, toAccountId, amount, LocalDateTime.now());
        ledgerEntryRepository.saveAll(Arrays.asList(debit, credit));

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txId);
        long sum = entries.stream().mapToLong(LedgerEntry::getAmount).sum();
        if (sum != 0) {
            throw new IllegalStateException("CRITICAL: Ledger entries do not sum to zero for txId=" + txId);
        }

        log.info("Transfer completed: txId={}, from={}, to={}, amount={}", txId, fromAccountId, toAccountId, amount);
        return transaction;
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
    }
}
