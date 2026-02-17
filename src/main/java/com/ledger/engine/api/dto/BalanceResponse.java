package com.ledger.engine.api.dto;

import java.util.UUID;

public class BalanceResponse {

    private UUID accountId;
    private long balance;

    public BalanceResponse() {
    }

    public BalanceResponse(UUID accountId, long balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }
}
