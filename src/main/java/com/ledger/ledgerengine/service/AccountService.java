package com.ledger.ledgerengine.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ledger.ledgerengine.domain.Account;
import com.ledger.ledgerengine.domain.LedgerEntry;
import com.ledger.ledgerengine.repository.AccountRepository;
import com.ledger.ledgerengine.repository.LedgerRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    public AccountService(AccountRepository accountRepository, LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public Account createAccount(String currency) {
        Account account = new Account(
                UUID.randomUUID(),
                currency,
                "ACTIVE",
                Instant.now());
        accountRepository.save(account);
        return account;
    }

    public Long getBalance(UUID accountId) {
        // In a real system we would verify account existence here
        return ledgerRepository.getBalance(accountId);
    }

    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<LedgerEntry> getAccountTransactions(UUID accountId) {
        // Verify account exists
        getAccount(accountId);
        return ledgerRepository.findByAccountId(accountId);
    }
}
