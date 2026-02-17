package com.ledger.engine;

import com.ledger.engine.domain.Account;
import com.ledger.engine.domain.Transaction;
import com.ledger.engine.exception.InsufficientFundsException;
import com.ledger.engine.service.AccountService;
import com.ledger.engine.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DepositWithdrawIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void deposit_shouldIncreaseBalance() {
        Account account = accountService.createAccount();

        ledgerService.deposit(account.getAccountId(), 10000L, UUID.randomUUID().toString());

        long balance = accountService.getBalance(account.getAccountId());
        assertEquals(10000L, balance);
    }

    @Test
    void multipleDeposits_shouldAccumulate() {
        Account account = accountService.createAccount();

        ledgerService.deposit(account.getAccountId(), 5000L, UUID.randomUUID().toString());
        ledgerService.deposit(account.getAccountId(), 3000L, UUID.randomUUID().toString());
        ledgerService.deposit(account.getAccountId(), 2000L, UUID.randomUUID().toString());

        long balance = accountService.getBalance(account.getAccountId());
        assertEquals(10000L, balance);
    }

    @Test
    void withdraw_shouldDecreaseBalance() {
        Account account = accountService.createAccount();
        ledgerService.deposit(account.getAccountId(), 10000L, UUID.randomUUID().toString());

        ledgerService.withdraw(account.getAccountId(), 3000L, UUID.randomUUID().toString());

        long balance = accountService.getBalance(account.getAccountId());
        assertEquals(7000L, balance);
    }

    @Test
    void withdraw_exactBalance_shouldSucceed() {
        Account account = accountService.createAccount();
        ledgerService.deposit(account.getAccountId(), 5000L, UUID.randomUUID().toString());

        ledgerService.withdraw(account.getAccountId(), 5000L, UUID.randomUUID().toString());

        long balance = accountService.getBalance(account.getAccountId());
        assertEquals(0L, balance);
    }

    @Test
    void withdraw_insufficientFunds_shouldThrow() {
        Account account = accountService.createAccount();
        ledgerService.deposit(account.getAccountId(), 1000L, UUID.randomUUID().toString());

        assertThrows(InsufficientFundsException.class,
                () -> ledgerService.withdraw(account.getAccountId(), 2000L, UUID.randomUUID().toString()));

        long balance = accountService.getBalance(account.getAccountId());
        assertEquals(1000L, balance);
    }

    @Test
    void withdraw_fromEmptyAccount_shouldThrow() {
        Account account = accountService.createAccount();

        assertThrows(InsufficientFundsException.class,
                () -> ledgerService.withdraw(account.getAccountId(), 100L, UUID.randomUUID().toString()));
    }

    @Test
    void deposit_negativeAmount_shouldThrow() {
        Account account = accountService.createAccount();

        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.deposit(account.getAccountId(), -100L, UUID.randomUUID().toString()));
    }

    @Test
    void withdraw_negativeAmount_shouldThrow() {
        Account account = accountService.createAccount();

        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.withdraw(account.getAccountId(), -100L, UUID.randomUUID().toString()));
    }

    @Test
    void deposit_returnsCompletedTransaction() {
        Account account = accountService.createAccount();

        Transaction tx = ledgerService.deposit(account.getAccountId(), 5000L, UUID.randomUUID().toString());

        assertNotNull(tx.getTransactionId());
        assertEquals("COMPLETED", tx.getStatus().name());
    }
}
