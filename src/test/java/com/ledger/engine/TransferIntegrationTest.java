package com.ledger.engine;

import com.ledger.engine.domain.Account;
import com.ledger.engine.exception.InsufficientFundsException;
import com.ledger.engine.repository.LedgerEntryRepository;
import com.ledger.engine.service.AccountService;
import com.ledger.engine.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransferIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void transfer_shouldMoveMoneyBetweenAccounts() {
        Account accountA = accountService.createAccount();
        Account accountB = accountService.createAccount();

        ledgerService.deposit(accountA.getAccountId(), 10000L, UUID.randomUUID().toString());
        ledgerService.transfer(accountA.getAccountId(), accountB.getAccountId(),
                3000L, UUID.randomUUID().toString());

        assertEquals(7000L, accountService.getBalance(accountA.getAccountId()));
        assertEquals(3000L, accountService.getBalance(accountB.getAccountId()));
    }

    @Test
    void transfer_insufficientFunds_shouldThrowAndNotMoveMoney() {
        Account accountA = accountService.createAccount();
        Account accountB = accountService.createAccount();

        ledgerService.deposit(accountA.getAccountId(), 1000L, UUID.randomUUID().toString());

        assertThrows(InsufficientFundsException.class,
                () -> ledgerService.transfer(accountA.getAccountId(), accountB.getAccountId(),
                        5000L, UUID.randomUUID().toString()));

        assertEquals(1000L, accountService.getBalance(accountA.getAccountId()));
        assertEquals(0L, accountService.getBalance(accountB.getAccountId()));
    }

    @Test
    void transfer_toSameAccount_shouldThrow() {
        Account account = accountService.createAccount();
        ledgerService.deposit(account.getAccountId(), 10000L, UUID.randomUUID().toString());

        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.transfer(account.getAccountId(), account.getAccountId(),
                        1000L, UUID.randomUUID().toString()));
    }

    @Test
    void transfer_zeroSumInvariant_shouldHold() {
        Account a = accountService.createAccount();
        Account b = accountService.createAccount();
        Account c = accountService.createAccount();

        ledgerService.deposit(a.getAccountId(), 10000L, UUID.randomUUID().toString());

        ledgerService.transfer(a.getAccountId(), b.getAccountId(), 3000L, UUID.randomUUID().toString());
        ledgerService.transfer(b.getAccountId(), c.getAccountId(), 1000L, UUID.randomUUID().toString());
        ledgerService.transfer(a.getAccountId(), c.getAccountId(), 2000L, UUID.randomUUID().toString());

        long totalBalance = accountService.getBalance(a.getAccountId())
                + accountService.getBalance(b.getAccountId())
                + accountService.getBalance(c.getAccountId());
        assertEquals(10000L, totalBalance);

        assertEquals(5000L, accountService.getBalance(a.getAccountId()));
        assertEquals(2000L, accountService.getBalance(b.getAccountId()));
        assertEquals(3000L, accountService.getBalance(c.getAccountId()));
    }

    @Test
    void multipleTransfers_totalMoneyShouldBeConserved() {
        Account a = accountService.createAccount();
        Account b = accountService.createAccount();

        ledgerService.deposit(a.getAccountId(), 50000L, UUID.randomUUID().toString());
        ledgerService.deposit(b.getAccountId(), 30000L, UUID.randomUUID().toString());

        ledgerService.transfer(a.getAccountId(), b.getAccountId(), 10000L, UUID.randomUUID().toString());
        ledgerService.transfer(b.getAccountId(), a.getAccountId(), 5000L, UUID.randomUUID().toString());
        ledgerService.transfer(a.getAccountId(), b.getAccountId(), 20000L, UUID.randomUUID().toString());

        long totalMoney = accountService.getBalance(a.getAccountId())
                + accountService.getBalance(b.getAccountId());
        assertEquals(80000L, totalMoney);
    }
}
