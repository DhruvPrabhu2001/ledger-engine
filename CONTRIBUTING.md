# Contributing

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USER/banking-engine.git`
3. Create a feature branch: `git checkout -b feature/my-feature`
4. Make your changes
5. Run tests: `mvn clean test` (requires Docker for Testcontainers)
6. Commit: `git commit -m "Add feature X"`
7. Push: `git push origin feature/my-feature`
8. Open a Pull Request

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for running tests with Testcontainers)
- PostgreSQL 15 (or use Docker Compose)

## Code Style

- No ORM — all data access uses `JdbcTemplate` with explicit SQL
- Ledger entries are immutable — never add UPDATE or DELETE operations
- Business logic belongs in the service layer, not domain objects or repositories
- All money movement must happen inside `@Transactional` methods in `LedgerService`
- Use `BIGINT` (long) for all monetary values — no floating point

## Testing Requirements

- All tests must use Testcontainers with a real PostgreSQL instance
- No mocking of the database for core logic tests
- New features must include invariant verification tests
- Concurrency tests are required for any operation that modifies account balances

## Architecture Rules

Before contributing, read [ARCHITECTURE.md](docs/ARCHITECTURE.md) and [INVARIANTS.md](docs/INVARIANTS.md). Key rules:

1. Ledger entries are never updated or deleted
2. Balances are always derived from `SUM(ledger_entries)`
3. Accounts must be locked (`SELECT FOR UPDATE`) before balance checks
4. Multiple accounts must be locked in sorted order (by UUID)
5. Every money movement method must check idempotency first
6. Transfer ledger entries must sum to zero

## Pull Request Checklist

- [ ] Code compiles without warnings
- [ ] All existing tests pass
- [ ] New tests added for new functionality
- [ ] Invariant tests updated if new invariants introduced
- [ ] Documentation updated if API changes
- [ ] No floating-point math used for money
- [ ] No `UPDATE` or `DELETE` on `ledger_entry` table
