package com.ledger.engine;

import com.ledger.engine.domain.Account;
import com.ledger.engine.exception.DuplicateRequestException;
import com.ledger.engine.service.AccountService;
import com.ledger.engine.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void duplicateDeposit_shouldThrowDuplicateRequestException() {
        Account account = accountService.createAccount();
        String key = UUID.randomUUID().toString();

        ledgerService.deposit(account.getAccountId(), 5000L, key);

        assertThrows(DuplicateRequestException.class, () -> ledgerService.deposit(account.getAccountId(), 5000L, key));

        assertEquals(5000L, accountService.getBalance(account.getAccountId()));
    }

    @Test
    void duplicateWithdraw_shouldThrowDuplicateRequestException() {
        Account account = accountService.createAccount();
        ledgerService.deposit(account.getAccountId(), 10000L, UUID.randomUUID().toString());

        String key = UUID.randomUUID().toString();
        ledgerService.withdraw(account.getAccountId(), 3000L, key);

        assertThrows(DuplicateRequestException.class, () -> ledgerService.withdraw(account.getAccountId(), 3000L, key));

        assertEquals(7000L, accountService.getBalance(account.getAccountId()));
    }

    @Test
    void duplicateTransfer_shouldThrowDuplicateRequestException() {
        Account a = accountService.createAccount();
        Account b = accountService.createAccount();
        ledgerService.deposit(a.getAccountId(), 10000L, UUID.randomUUID().toString());

        String key = UUID.randomUUID().toString();
        ledgerService.transfer(a.getAccountId(), b.getAccountId(), 3000L, key);

        assertThrows(DuplicateRequestException.class,
                () -> ledgerService.transfer(a.getAccountId(), b.getAccountId(), 3000L, key));

        assertEquals(7000L, accountService.getBalance(a.getAccountId()));
        assertEquals(3000L, accountService.getBalance(b.getAccountId()));
    }

    @Test
    void differentKeys_shouldBothSucceed() {
        Account account = accountService.createAccount();

        ledgerService.deposit(account.getAccountId(), 5000L, UUID.randomUUID().toString());
        ledgerService.deposit(account.getAccountId(), 5000L, UUID.randomUUID().toString());

        assertEquals(10000L, accountService.getBalance(account.getAccountId()));
    }

    @Test
    void duplicateRequest_shouldReturnExistingTransactionId() {
        Account account = accountService.createAccount();
        String key = UUID.randomUUID().toString();

        var tx = ledgerService.deposit(account.getAccountId(), 5000L, key);

        DuplicateRequestException ex = assertThrows(DuplicateRequestException.class,
                () -> ledgerService.deposit(account.getAccountId(), 5000L, key));

        assertEquals(tx.getTransactionId(), ex.getExistingTransactionId());
    }
}
