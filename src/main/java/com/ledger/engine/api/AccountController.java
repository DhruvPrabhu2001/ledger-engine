package com.ledger.engine.api;

import com.ledger.engine.api.dto.AccountResponse;
import com.ledger.engine.api.dto.BalanceResponse;
import com.ledger.engine.api.dto.LedgerEntryResponse;
import com.ledger.engine.domain.Account;
import com.ledger.engine.domain.LedgerEntry;
import com.ledger.engine.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount() {
        Account account = accountService.createAccount();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        Account account = accountService.getAccount(accountId);
        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        List<AccountResponse> accounts = accountService.listAccounts().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId) {
        long balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(new BalanceResponse(accountId, balance));
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<LedgerEntryResponse>> getTransactions(@PathVariable UUID accountId) {
        List<LedgerEntryResponse> entries = accountService.getAccountTransactions(accountId).stream()
                .map(this::toEntryResponse)
                .toList();
        return ResponseEntity.ok(entries);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getStatus().name(),
                account.getCreatedAt());
    }

    private LedgerEntryResponse toEntryResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getLedgerEntryId(),
                entry.getTransactionId(),
                entry.getAmount(),
                entry.getCreatedAt());
    }
}
