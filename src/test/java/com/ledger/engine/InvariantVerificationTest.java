package com.ledger.engine;

import com.ledger.engine.domain.Account;
import com.ledger.engine.repository.LedgerEntryRepository;
import com.ledger.engine.service.AccountService;
import com.ledger.engine.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvariantVerificationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void globalInvariant_totalDepositsEqualGlobalSum() {
        Account a = accountService.createAccount();
        Account b = accountService.createAccount();
        Account c = accountService.createAccount();

        long totalDeposited = 0;
        ledgerService.deposit(a.getAccountId(), 10000L, UUID.randomUUID().toString());
        totalDeposited += 10000L;
        ledgerService.deposit(b.getAccountId(), 20000L, UUID.randomUUID().toString());
        totalDeposited += 20000L;
        ledgerService.deposit(c.getAccountId(), 5000L, UUID.randomUUID().toString());
        totalDeposited += 5000L;

        ledgerService.transfer(a.getAccountId(), b.getAccountId(), 3000L, UUID.randomUUID().toString());
        ledgerService.transfer(b.getAccountId(), c.getAccountId(), 5000L, UUID.randomUUID().toString());
        ledgerService.transfer(c.getAccountId(), a.getAccountId(), 2000L, UUID.randomUUID().toString());

        ledgerService.withdraw(a.getAccountId(), 1000L, UUID.randomUUID().toString());
        totalDeposited -= 1000L;

        long globalSum = ledgerEntryRepository.globalLedgerSum();
        assertEquals(totalDeposited, globalSum);

        long totalBalance = accountService.getBalance(a.getAccountId())
                + accountService.getBalance(b.getAccountId())
                + accountService.getBalance(c.getAccountId());
        assertEquals(totalDeposited, totalBalance);
        assertEquals(globalSum, totalBalance);
    }

    @Test
    void transferEntries_shouldAlwaysSumToZero() {
        Account a = accountService.createAccount();
        Account b = accountService.createAccount();

        ledgerService.deposit(a.getAccountId(), 10000L, UUID.randomUUID().toString());

        var tx = ledgerService.transfer(a.getAccountId(), b.getAccountId(),
                3000L, UUID.randomUUID().toString());

        var entries = ledgerEntryRepository.findByTransactionId(tx.getTransactionId());
        long entrySum = entries.stream().mapToLong(e -> e.getAmount()).sum();

        assertEquals(0L, entrySum, "Transfer entries must sum to zero");
        assertEquals(2, entries.size(), "Transfer should create exactly 2 entries");
    }
}
