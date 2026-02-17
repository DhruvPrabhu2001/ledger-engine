# Project Walkthrough

This document explains the entire Ledger-Based Banking Engine in enough detail that a new backend engineer can understand the system without reading the source code.

---

## Table of Contents
1. [What This System Does](#what-this-system-does)
2. [Database Tables](#database-tables)
3. [How Balances Work](#how-balances-work)
4. [How Deposits Work](#how-deposits-work)
5. [How Withdrawals Work](#how-withdrawals-work)
6. [How Transfers Work](#how-transfers-work)
7. [How Idempotency Works](#how-idempotency-works)
8. [How Concurrency Is Handled](#how-concurrency-is-handled)
9. [How to Reason About Failures](#how-to-reason-about-failures)
10. [Design Decisions Explained](#design-decisions-explained)
11. [Example Failure Scenarios](#example-failure-scenarios)

---

## What This System Does

This is a **money movement core engine**. It tracks how money moves between accounts using an immutable ledger — similar to how banks and payment systems work internally.

The system does NOT:
- Have a UI
- Manage users or authentication
- Handle currency conversion
- Process real payments

It DOES:
- Create accounts
- Record deposits, withdrawals, and transfers as immutable ledger entries
- Derive balances by summing those entries
- Prevent double-processing via idempotency
- Handle concurrent access safely
- Guarantee atomicity — all or nothing

---

## Database Tables

### `account`

| Column | Type | Description |
|--------|------|-------------|
| `account_id` | UUID (PK) | Unique account identifier |
| `status` | VARCHAR(20) | `ACTIVE` or `CLOSED` |
| `created_at` | TIMESTAMP | When the account was created |

**Important**: There is NO `balance` column. Balances are derived.

### `transaction`

| Column | Type | Description |
|--------|------|-------------|
| `transaction_id` | UUID (PK) | Unique transaction identifier |
| `idempotency_key` | VARCHAR(255) | Client-provided unique key (UNIQUE index) |
| `status` | VARCHAR(20) | `INITIATED`, `COMPLETED`, or `FAILED` |
| `created_at` | TIMESTAMP | When the transaction was created |

**Important**: The `idempotency_key` has a UNIQUE constraint. This is the database-level guarantee against double-processing.

### `ledger_entry`

| Column | Type | Description |
|--------|------|-------------|
| `ledger_entry_id` | UUID (PK) | Unique entry identifier |
| `transaction_id` | UUID (FK) | Links to the parent transaction |
| `account_id` | UUID (FK) | Which account this entry affects |
| `amount` | BIGINT | Signed amount: positive=credit, negative=debit |
| `created_at` | TIMESTAMP | When the entry was created |

**Important**: Entries are NEVER updated or deleted. This is the heart of the system. Foreign keys use `ON DELETE RESTRICT` — you cannot delete an account that has ledger entries.

---

## How Balances Work

The balance of any account is computed as:

```sql
SELECT COALESCE(SUM(amount), 0) FROM ledger_entry WHERE account_id = ?
```

This means:
- A new account with no entries has balance = 0
- A deposit of 10000 adds an entry with amount = +10000, so balance = 10000
- A withdrawal of 3000 adds an entry with amount = -3000, so balance = 7000
- The balance is ALWAYS the sum of ALL entries for that account

**Why not store the balance?** Because then you have two sources of truth: the stored balance and the actual entries. If they ever disagree (due to a bug, partial failure, or data corruption), you don't know which one is correct. By deriving, the entries ARE the truth.

---

## How Deposits Work

1. Client sends: `{ accountId, amount, idempotencyKey }`
2. System checks if `idempotencyKey` already exists → reject if duplicate
3. System locks the account row with `SELECT FOR UPDATE`
4. System creates a `transaction` record with status `COMPLETED`
5. System creates a `ledger_entry` with `+amount`
6. Everything commits in one database transaction

A deposit creates money from outside the system. After a deposit of 10000, the global ledger sum increases by 10000.

---

## How Withdrawals Work

1. Client sends: `{ accountId, amount, idempotencyKey }`
2. System checks idempotency → reject if duplicate
3. System locks the account row
4. System computes derived balance: `SUM(amount) WHERE account_id = ?`
5. If balance < amount → reject with `INSUFFICIENT_FUNDS`
6. System creates a `transaction` + `ledger_entry` with `-amount`
7. Commits

**Critical order**: The account is locked BEFORE the balance is checked. This prevents two concurrent withdrawals from both seeing "balance = 1000" and both withdrawing 1000, which would create a -1000 balance.

---

## How Transfers Work

1. Client sends: `{ fromAccountId, toAccountId, amount, idempotencyKey }`
2. System checks idempotency → reject if duplicate
3. System sorts the two account IDs and locks them IN ORDER
4. System computes the source account's derived balance
5. If balance < amount → reject with `INSUFFICIENT_FUNDS`
6. System creates one `transaction` record
7. System creates TWO `ledger_entry` records:
   - Source: `-amount` (debit)
   - Destination: `+amount` (credit)
8. System reads back the entries and asserts their sum = 0
9. Commits

**Why sorted locking?** Consider two threads:
- Thread 1: Transfer A → B (locks A, then B)
- Thread 2: Transfer B → A (locks B, then A)

Without sorting, Thread 1 holds A and waits for B, while Thread 2 holds B and waits for A → **deadlock**. By sorting, both threads lock in the same order (e.g., A then B), so one waits for the other to finish — no deadlock.

---

## How Idempotency Works

Every money movement request requires a unique `idempotencyKey`. This is the client's responsibility to generate (typically a UUID).

**Step by step:**
1. Before doing any work, the service calls `findByIdempotencyKey(key)`
2. If a transaction with this key already exists, a `DuplicateRequestException` is thrown
3. The exception contains the existing `transactionId` so the client knows their original request was processed
4. If no existing transaction is found, processing continues normally

**Database safety net**: The `transaction` table has a `UNIQUE` constraint on `idempotency_key`. Even if the application-level check has a race condition (two requests arrive at the exact same instant), the database will reject the second `INSERT`, and the entire transaction will roll back.

**Why this matters**: Imagine a client sends a deposit request but the network drops before they receive the response. They don't know if the deposit was processed. With idempotency, they can safely retry with the SAME key. Either:
- The first request was processed → they get a `409 Conflict` with the existing transaction ID
- The first request was NOT processed → the retry goes through normally

---

## How Concurrency Is Handled

### Pessimistic Locking
The system uses `SELECT FOR UPDATE` to acquire row-level locks on accounts. While an account is locked:
- Other transactions touching that account must WAIT
- The lock is held until the transaction COMMITS or ROLLS BACK

### Why Not Optimistic Locking?
Optimistic locking (read-check-write with version numbers) leads to retry storms under high load. In a financial system, you want predictable behavior: if two requests compete, one waits for the other. Pessimistic locking gives this guarantee.

### What About Database Connection Pool Exhaustion?
If many concurrent requests lock and wait, they consume database connections. The connection pool is configured with `spring.datasource.hikari.maximum-pool-size=10` by default. Under extreme load, excess requests will get a connection timeout. This is the correct behavior — it's better to reject excess requests than to lose money.

---

## How to Reason About Failures

### Failure During Processing
If the application crashes mid-transaction (e.g., after inserting the transaction but before inserting ledger entries), PostgreSQL automatically ROLLS BACK the uncommitted transaction. Nothing is left in a half-written state.

### Failure After Commit
If the application crashes after COMMIT but before sending the response to the client, the client doesn't know the request succeeded. They can retry with the same `idempotencyKey` and will get a `409 Conflict` — confirming the original request was processed.

### Database Failure
If PostgreSQL goes down during processing, all in-flight transactions are rolled back. When it comes back up, the data is consistent.

### Network Partition
If the network between the app and the database fails mid-transaction, the database will eventually timeout and roll back. The client can retry.

### Summary Table

| Failure Point | Data State | Recovery |
|---|---|---|
| Before COMMIT | Rolled back | Retry with same key |
| After COMMIT, before response | Committed | Retry → 409 Conflict |
| Database crash | Rolled back | Retry with same key |
| App crash after COMMIT | Committed | Retry → 409 Conflict |

---

## Design Decisions Explained

### Why BIGINT, Not DECIMAL?
Floating-point arithmetic has rounding errors. `0.1 + 0.2 != 0.3` in IEEE 754. By using BIGINT (cents/smallest unit), all arithmetic is exact integer math. 1000 = $10.00.

### Why No Balance Column?
Explained above. Two sources of truth are dangerous in financial systems.

### Why No JPA/Hibernate?
- We need `SELECT FOR UPDATE` with precise control
- JPA hides SQL behind abstractions — in a financial system, you want to see exactly what queries run
- Only 3 tables with simple queries — ORM adds complexity without benefit
- No lazy loading, no N+1 queries, no unexpected flushes

### Why Spring Boot?
- Provides `@Transactional`, connection pooling (HikariCP), REST infrastructure
- Well-tested and production-proven
- We use Spring JDBC, not Spring Data JPA

### Why Flyway?
Schema must be version-controlled. Flyway runs migrations on startup, ensuring the database schema is always in sync with the code.

### Why Testcontainers?
- Tests run against a REAL PostgreSQL instance, not H2 or mocks
- Ensures SQL syntax, constraints, and locking behavior match production
- Containers are ephemeral — each test class gets a clean database

---

## Example Failure Scenarios

### Scenario 1: Network Timeout on Deposit
```
Client → deposit(account=A, amount=10000, key="abc-123")
           ↓
     [network timeout — client doesn't receive response]
           ↓
Client → deposit(account=A, amount=10000, key="abc-123")  ← RETRY
           ↓
     Service finds existing transaction with key="abc-123"
           ↓
     Returns 409 Conflict + existing transactionId
           ↓
Client knows: the original deposit was processed. Balance = 10000, not 20000.
```

### Scenario 2: Concurrent Withdrawals Race
```
Account A: balance = 1000

Thread 1 → withdraw(A, 1000, key1)
Thread 2 → withdraw(A, 1000, key2)

Thread 1: LOCK account A → balance=1000 → withdraw → balance=0 → COMMIT
Thread 2: LOCK account A (was waiting) → balance=0 → INSUFFICIENT_FUNDS → ROLLBACK

Result: Only one withdrawal succeeds. Balance = 0.
```

### Scenario 3: App Crash Mid-Transfer
```
Transfer A → B, amount=5000

1. Lock A ✓
2. Lock B ✓
3. Insert transaction ✓
4. Insert entry A=-5000 ✓
5. [APP CRASHES HERE]
6. Insert entry B=+5000 ✗ (never executed)

Result: PostgreSQL ROLLS BACK the entire transaction.
         A still has original balance. B unchanged.
         Client retries with same key → processes normally.
```

### Scenario 4: Deadlock Prevention
```
Without sorted locking:
  Thread 1: Lock A then B
  Thread 2: Lock B then A
  → DEADLOCK

With sorted locking (A < B):
  Thread 1: Lock A then B
  Thread 2: Lock A then B  ← same order!
  → Thread 2 waits for Thread 1 to finish. No deadlock.
```
