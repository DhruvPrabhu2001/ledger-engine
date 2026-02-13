package com.ledger.moneyengine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ledger.ledgerengine.domain.Account;
import com.ledger.ledgerengine.dto.TransferRequest;
import com.ledger.ledgerengine.service.AccountService;
import com.ledger.ledgerengine.service.LedgerService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LedgerConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void testConcurrentTransfers() throws InterruptedException {
        // Setup Accounts
        Account treasury = accountService.createAccount("USD");
        Account accountA = accountService.createAccount("USD");
        Account accountB = accountService.createAccount("USD");

        // Initial Funding: Treasury -> A ($1000)
        ledgerService.processTransaction(new TransferRequest(
                UUID.randomUUID().toString(),
                List.of(
                        new TransferRequest.TransferEntry(treasury.accountId(), -1000),
                        new TransferRequest.TransferEntry(accountA.accountId(), 1000))));

        int threadCount = 10;
        int amountPerTransfer = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Try to transfer 100 from A -> B
                    ledgerService.processTransaction(new TransferRequest(
                            UUID.randomUUID().toString(),
                            List.of(
                                    new TransferRequest.TransferEntry(accountA.accountId(), -amountPerTransfer),
                                    new TransferRequest.TransferEntry(accountB.accountId(), amountPerTransfer))));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long balanceA = accountService.getBalance(accountA.accountId());
        long balanceB = accountService.getBalance(accountB.accountId());

        System.out.println("Success: " + successCount.get());
        System.out.println("Fail: " + failCount.get());
        System.out.println("Balance A: " + balanceA);
        System.out.println("Balance B: " + balanceB);

        // A started with 1000.
        // If all 10 threads succeed, A should be 0, B should be 1000.
        // Total money in system (A+B) should be 1000 (Treasury excluded from check).

        long expectedB = successCount.get() * amountPerTransfer;
        long expectedA = 1000 - expectedB;

        assertEquals(expectedA, balanceA, "Account A balance mismatch");
        assertEquals(expectedB, balanceB, "Account B balance mismatch");
        assertEquals(1000, balanceA + balanceB, "Total money invariant violated");
    }
}
