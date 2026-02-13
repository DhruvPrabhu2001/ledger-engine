package com.ledger.ledgerengine.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ledger.ledgerengine.domain.Account;
import com.ledger.ledgerengine.domain.LedgerEntry;
import com.ledger.ledgerengine.service.AccountService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Map<String, String> request) {
        String currency = request.getOrDefault("currency", "USD");
        Account account = accountService.createAccount(currency);
        return ResponseEntity.ok(account);
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID accountId) {
        Long balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance));
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<LedgerEntry>> getTransactions(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccountTransactions(accountId));
    }
}
