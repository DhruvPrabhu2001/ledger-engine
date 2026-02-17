# Architecture

## Overview

The Ledger-Based Banking Engine follows a strict layered architecture with clear separation of concerns.

```
┌─────────────────────────────────┐
│         REST API Layer          │  ← Controllers, DTOs, Exception Handler
├─────────────────────────────────┤
│         Service Layer           │  ← Business logic, transaction management
├─────────────────────────────────┤
│       Repository Layer          │  ← JDBC data access, SQL queries
├─────────────────────────────────┤
│      PostgreSQL Database        │  ← Immutable ledger, constraints, locks
└─────────────────────────────────┘
```

## Layer Responsibilities

### API Layer (`com.ledger.engine.api`)
- REST endpoints for accounts and transactions
- Request validation and response formatting
- Maps domain exceptions to HTTP status codes
- **No business logic lives here**

### Service Layer (`com.ledger.engine.service`)
- `AccountService` — Account lifecycle (create, query, balance derivation)
- `LedgerService` — Core money movement engine (deposit, withdraw, transfer)
- Transaction boundary management (`@Transactional`)
- Idempotency enforcement
- Balance validation
- **All business rules are enforced here**

### Repository Layer (`com.ledger.engine.repository`)
- Pure data access via `JdbcTemplate`
- `AccountRepository` — CRUD + pessimistic locking (`SELECT FOR UPDATE`)
- `TransactionRepository` — CRUD + idempotency key lookup
- `LedgerEntryRepository` — Insert-only + balance derivation + audit queries
- **No business logic, only SQL**

### Domain Layer (`com.ledger.engine.domain`)
- Plain Java objects: `Account`, `Transaction`, `LedgerEntry`
- Enums: `AccountStatus`, `TransactionStatus`
- **No behavior, no business logic** (per spec: "Account entity must not contain business logic")

## Data Flow

### Deposit
```
Client → AccountController → LedgerService.deposit()
  1. Check idempotency_key
  2. SELECT FOR UPDATE on account
  3. INSERT transaction (status=COMPLETED)
  4. INSERT ledger_entry (+amount)
  5. COMMIT
```

### Transfer
```
Client → TransactionController → LedgerService.transfer()
  1. Check idempotency_key
  2. SELECT FOR UPDATE on BOTH accounts (sorted by UUID)
  3. Derive source balance, validate >= amount
  4. INSERT transaction (status=COMPLETED)
  5. INSERT ledger_entry (-amount for source)
  6. INSERT ledger_entry (+amount for destination)
  7. Assert SUM(entries) = 0
  8. COMMIT
```

## Database Schema

```
┌──────────────────┐       ┌──────────────────┐
│     account      │       │   transaction    │
├──────────────────┤       ├──────────────────┤
│ account_id (PK)  │       │ transaction_id   │
│ status           │       │ idempotency_key  │
│ created_at       │       │ status           │
└────────┬─────────┘       │ created_at       │
         │                 └────────┬─────────┘
         │                          │
         │    ┌─────────────────┐   │
         └───→│  ledger_entry   │←──┘
              ├─────────────────┤
              │ ledger_entry_id │
              │ transaction_id  │
              │ account_id      │
              │ amount (BIGINT) │
              │ created_at      │
              └─────────────────┘
```

## Why JDBC, Not JPA

- Full control over SQL queries and lock semantics
- Explicit `SELECT FOR UPDATE` impossible to guarantee in JPA
- No "magic" — every query is visible and auditable
- Performance: no N+1 queries, no lazy loading surprises
- The system has exactly 3 tables; an ORM adds complexity with no benefit

## Concurrency Model

The engine uses **pessimistic locking** via `SELECT FOR UPDATE`:

1. Before any money movement, the involved account rows are locked
2. Multiple accounts are locked in **sorted UUID order** to prevent deadlocks
3. The lock is held for the duration of the transaction
4. Other threads attempting to lock the same accounts will wait (serialized execution)
5. This guarantees that balance checks and writes are atomic

## Idempotency Model

1. Every request carries an `idempotency_key`
2. Before processing, the service checks if a transaction with this key already exists
3. If it exists, a `DuplicateRequestException` is thrown with the existing transaction ID
4. The database has a `UNIQUE` constraint on `idempotency_key` as a safety net
5. Clients can safely retry requests — double processing is impossible
