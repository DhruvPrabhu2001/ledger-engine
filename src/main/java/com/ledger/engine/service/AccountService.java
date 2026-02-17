package com.ledger.engine.service;

import com.ledger.engine.domain.Account;
import com.ledger.engine.domain.AccountStatus;
import com.ledger.engine.domain.LedgerEntry;
import com.ledger.engine.exception.AccountNotFoundException;
import com.ledger.engine.repository.AccountRepository;
import com.ledger.engine.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Account createAccount() {
        Account account = new Account(UUID.randomUUID(), AccountStatus.ACTIVE, LocalDateTime.now());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long getBalance(UUID accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));
        return ledgerEntryRepository.deriveBalance(accountId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getAccountTransactions(UUID accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));
        return ledgerEntryRepository.findByAccountId(accountId);
    }
}
