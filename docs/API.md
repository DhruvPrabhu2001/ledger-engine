# API Reference

Base URL: `http://localhost:8080`

## Accounts

### Create Account
```
POST /api/accounts
```

**Response** `201 Created`:
```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "createdAt": "2026-02-15T20:00:00"
}
```

---

### Get Account
```
GET /api/accounts/{accountId}
```

**Response** `200 OK`:
```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "createdAt": "2026-02-15T20:00:00"
}
```

**Error** `404 Not Found`:
```json
{
  "error": "ACCOUNT_NOT_FOUND",
  "message": "Account not found: 550e8400-...",
  "timestamp": "2026-02-15T20:00:00"
}
```

---

### List Accounts
```
GET /api/accounts
```

**Response** `200 OK`:
```json
[
  {
    "accountId": "550e8400-...",
    "status": "ACTIVE",
    "createdAt": "2026-02-15T20:00:00"
  }
]
```

---

### Get Balance
```
GET /api/accounts/{accountId}/balance
```

**Response** `200 OK`:
```json
{
  "accountId": "550e8400-...",
  "balance": 10000
}
```

> Balance is in the smallest currency unit (e.g., cents). Always derived from ledger entries.

---

### Get Transaction History
```
GET /api/accounts/{accountId}/transactions
```

**Response** `200 OK`:
```json
[
  {
    "ledgerEntryId": "...",
    "transactionId": "...",
    "accountId": "...",
    "amount": 10000,
    "createdAt": "2026-02-15T20:00:00"
  },
  {
    "ledgerEntryId": "...",
    "transactionId": "...",
    "accountId": "...",
    "amount": -3000,
    "createdAt": "2026-02-15T20:01:00"
  }
]
```

---

## Transactions

### Deposit
```
POST /api/transactions/deposit
Content-Type: application/json
```

**Request**:
```json
{
  "accountId": "550e8400-...",
  "amount": 10000,
  "idempotencyKey": "dep-unique-key-1"
}
```

**Response** `201 Created`:
```json
{
  "transactionId": "...",
  "idempotencyKey": "dep-unique-key-1",
  "status": "COMPLETED",
  "createdAt": "2026-02-15T20:00:00"
}
```

---

### Withdraw
```
POST /api/transactions/withdraw
Content-Type: application/json
```

**Request**:
```json
{
  "accountId": "550e8400-...",
  "amount": 3000,
  "idempotencyKey": "wd-unique-key-1"
}
```

**Response** `201 Created`:
```json
{
  "transactionId": "...",
  "idempotencyKey": "wd-unique-key-1",
  "status": "COMPLETED",
  "createdAt": "2026-02-15T20:00:00"
}
```

**Error** `400 Bad Request` (insufficient funds):
```json
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds: balance=1000, requested=5000",
  "timestamp": "2026-02-15T20:00:00"
}
```

---

### Transfer
```
POST /api/transactions/transfer
Content-Type: application/json
```

**Request**:
```json
{
  "fromAccountId": "550e8400-...",
  "toAccountId": "660e9500-...",
  "amount": 5000,
  "idempotencyKey": "txfr-unique-key-1"
}
```

**Response** `201 Created`:
```json
{
  "transactionId": "...",
  "idempotencyKey": "txfr-unique-key-1",
  "status": "COMPLETED",
  "createdAt": "2026-02-15T20:00:00"
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "timestamp": "2026-02-15T20:00:00"
}
```

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `ACCOUNT_NOT_FOUND` | 404 | Account does not exist |
| `INSUFFICIENT_FUNDS` | 400 | Balance too low for operation |
| `DUPLICATE_REQUEST` | 409 | Idempotency key already used |
| `BAD_REQUEST` | 400 | Invalid input (negative amount, self-transfer, etc.) |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

## Idempotency

Every money movement endpoint requires an `idempotencyKey`:
- Use a UUID or client-generated unique string
- If the same key is sent twice, the second request returns `409 Conflict` with the original `transactionId`
- This makes all operations safe to retry
