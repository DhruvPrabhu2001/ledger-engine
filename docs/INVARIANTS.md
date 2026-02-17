# Invariants

This document defines every invariant the system enforces and explains why each exists.

## 1. Ledger Entries Are Immutable

**Rule**: Once a `ledger_entry` row is inserted, it is NEVER updated or deleted.

**Why**: Immutability guarantees auditability. If entries could be modified, there would be no reliable record of financial history. Every state change is expressed as a new entry, creating a complete audit trail.

**Enforcement**:
- Application code only calls `INSERT` on `ledger_entry`
- No `UPDATE` or `DELETE` methods exist in `LedgerEntryRepository`
- `ON DELETE RESTRICT` foreign keys prevent cascading deletes

---

## 2. Balances Are Derived, Never Stored

**Rule**: Account balances are always computed as `SELECT SUM(amount) FROM ledger_entry WHERE account_id = ?`.

**Why**: Storing balances as a separate field creates two sources of truth. If the balance column disagrees with the ledger entries, which one is correct? By deriving balances, the ledger entries ARE the truth.

**Enforcement**:
- `Account` domain object has no `balance` field
- `account` table has no `balance` column
- `LedgerEntryRepository.deriveBalance()` is the only way to get a balance

---

## 3. Money Conservation (Zero-Sum Transfers)

**Rule**: For every transfer transaction, the sum of all ledger entries must equal zero.

**Why**: Money cannot be created or destroyed during a transfer. If account A sends 1000 to account B, the entries must be: A=-1000, B=+1000, sum=0.

**Enforcement**:
- `LedgerService.transfer()` inserts exactly two entries: `-amount` and `+amount`
- After insertion, the service reads back the entries and asserts `sum == 0`
- If the assertion fails, the transaction is rolled back

---

## 4. Accounts Must Be Locked Before Balance Checks

**Rule**: Before reading a balance for a write operation, the account row must be locked with `SELECT FOR UPDATE`.

**Why**: Without locking, two concurrent transactions could both read balance=1000, then both withdraw 800, resulting in -600 (overdraft). The lock serializes access.

**Enforcement**:
- `AccountRepository.lockForUpdate()` uses `SELECT ... FOR UPDATE`
- `LedgerService` always calls `lockForUpdate()` before `deriveBalance()`
- Lock ordering (sorted by UUID) prevents deadlocks

---

## 5. Idempotency

**Rule**: Each `idempotency_key` can be used exactly once.

**Why**: Network failures, timeouts, and retries are inevitable. If a client sends the same deposit request twice (due to a timeout), the system must process it only once. Without idempotency, retries could double-charge users.

**Enforcement**:
- `UNIQUE` constraint on `transaction.idempotency_key` (database level)
- Application checks `findByIdempotencyKey()` before processing
- Duplicate requests throw `DuplicateRequestException` with the existing transaction ID

---

## 6. Atomicity

**Rule**: All money movement (transaction record + ledger entries) occurs inside a single database transaction.

**Why**: If the system crashes after inserting the transaction but before inserting entries, the data is inconsistent. Atomicity ensures all-or-nothing: either everything commits, or everything rolls back.

**Enforcement**:
- `LedgerService` methods are annotated with `@Transactional`
- A single PostgreSQL transaction wraps: lock → validate → insert transaction → insert entries → commit

---

## 7. No Overdraft

**Rule**: Withdrawals and transfers are rejected if the derived balance is less than the requested amount.

**Why**: In this system, accounts cannot go negative. This is a business rule enforced for correctness.

**Enforcement**:
- Before any debit operation, `deriveBalance()` is called (after locking)
- If `balance < amount`, `InsufficientFundsException` is thrown and the transaction rolls back

---

## 8. Account Entity Has No Business Logic

**Rule**: The `Account` class is a pure data holder (POJO). No methods perform validation, balance lookups, or state transitions.

**Why**: Business logic belongs in the service layer, not in domain objects. This keeps the domain model simple and testable, and prevents coupling data representation to behavior.

**Enforcement**:
- `Account.java` has only getters, setters, and constructors
- All logic lives in `AccountService` and `LedgerService`
