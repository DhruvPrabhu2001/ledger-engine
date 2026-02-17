package com.ledger.engine;

import com.ledger.engine.domain.Account;
import com.ledger.engine.service.AccountService;
import com.ledger.engine.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void parallelTransfers_sameDirection_shouldMaintainConsistency() throws Exception {
        Account accountA = accountService.createAccount();
        Account accountB = accountService.createAccount();
        ledgerService.deposit(accountA.getAccountId(), 10000L, UUID.randomUUID().toString());

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    ledgerService.transfer(
                            accountA.getAccountId(),
                            accountB.getAccountId(),
                            100L,
                            UUID.randomUUID().toString());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        latch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        long balanceA = accountService.getBalance(accountA.getAccountId());
        long balanceB = accountService.getBalance(accountB.getAccountId());

        assertEquals(10000L - (numThreads * 100L), balanceA);
        assertEquals(numThreads * 100L, balanceB);
        assertEquals(10000L, balanceA + balanceB);
    }

    @Test
    void parallelTransfers_bidirectional_noDeadlock() throws Exception {
        Account accountA = accountService.createAccount();
        Account accountB = accountService.createAccount();
        ledgerService.deposit(accountA.getAccountId(), 50000L, UUID.randomUUID().toString());
        ledgerService.deposit(accountB.getAccountId(), 50000L, UUID.randomUUID().toString());

        int numThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    if (idx % 2 == 0) {
                        ledgerService.transfer(accountA.getAccountId(), accountB.getAccountId(),
                                100L, UUID.randomUUID().toString());
                    } else {
                        ledgerService.transfer(accountB.getAccountId(), accountA.getAccountId(),
                                100L, UUID.randomUUID().toString());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        latch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        long balanceA = accountService.getBalance(accountA.getAccountId());
        long balanceB = accountService.getBalance(accountB.getAccountId());

        assertEquals(100000L, balanceA + balanceB);
    }

    @Test
    void concurrentWithdrawals_insufficientFundsRace() throws Exception {
        Account account = accountService.createAccount();
        ledgerService.deposit(account.getAccountId(), 1000L, UUID.randomUUID().toString());

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    ledgerService.withdraw(account.getAccountId(), 1000L, UUID.randomUUID().toString());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        latch.countDown();

        int successes = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(10, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        executor.shutdown();

        assertEquals(1, successes);
        assertEquals(0L, accountService.getBalance(account.getAccountId()));
    }
}
