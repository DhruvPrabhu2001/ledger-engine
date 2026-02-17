package com.ledger.engine;

import com.ledger.engine.domain.Account;
import com.ledger.engine.domain.LedgerEntry;
import com.ledger.engine.exception.AccountNotFoundException;
import com.ledger.engine.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Test
    void createAccount_shouldReturnNewActiveAccount() {
        Account account = accountService.createAccount();

        assertNotNull(account.getAccountId());
        assertEquals("ACTIVE", account.getStatus().name());
    }

    @Test
    void getAccount_shouldReturnExistingAccount() {
        Account created = accountService.createAccount();
        Account found = accountService.getAccount(created.getAccountId());

        assertEquals(created.getAccountId(), found.getAccountId());
        assertEquals("ACTIVE", found.getStatus().name());
    }

    @Test
    void getAccount_nonExistent_shouldThrow() {
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(java.util.UUID.randomUUID()));
    }

    @Test
    void listAccounts_shouldReturnAll() {
        int initialSize = accountService.listAccounts().size();
        accountService.createAccount();
        accountService.createAccount();

        List<Account> accounts = accountService.listAccounts();
        assertTrue(accounts.size() >= initialSize + 2);
    }

    @Test
    void getBalance_newAccount_shouldBeZero() {
        Account account = accountService.createAccount();
        long balance = accountService.getBalance(account.getAccountId());

        assertEquals(0L, balance);
    }

    @Test
    void getAccountTransactions_newAccount_shouldBeEmpty() {
        Account account = accountService.createAccount();
        List<LedgerEntry> entries = accountService.getAccountTransactions(account.getAccountId());

        assertTrue(entries.isEmpty());
    }
}
