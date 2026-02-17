# Ledger-Based Banking Engine

A production-quality, ledger-based banking engine built on strict accounting principles. Designed for correctness, auditability, and embeddability.

## Key Principles

- **Ledger entries are immutable** — no updates, no deletes
- **Balances are derived** — always computed from `SUM(ledger_entries)`, never stored
- **Money is never created or destroyed** — every transfer has equal debits and credits
- **Atomic transactions** — all money movement happens in a single DB transaction
- **Idempotent** — duplicate requests are safely rejected using database-level unique constraints
- **Concurrency-safe** — `SELECT FOR UPDATE` with sorted locking prevents deadlocks and race conditions

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Build | Maven |
| Framework | Spring Boot 3.2 (Web + JDBC) |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Data Access | JdbcTemplate (no ORM) |
| Testing | Testcontainers, JUnit 5 |
| Deployment | Docker, Docker Compose |

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Run with Docker Compose
```bash
docker-compose up --build
```
The app starts at `http://localhost:8080`.

### Run Locally (with external PostgreSQL)
```bash
# Set environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=banking_engine
export DB_USER=postgres
export DB_PASSWORD=postgres

mvn spring-boot:run
```

### Build
```bash
mvn clean package
```

### Run Tests (requires Docker for Testcontainers)
```bash
mvn clean test
```

## API Quick Reference

### Accounts
```bash
# Create account
curl -X POST http://localhost:8080/api/accounts

# Get balance
curl http://localhost:8080/api/accounts/{accountId}/balance

# List accounts
curl http://localhost:8080/api/accounts

# Transaction history
curl http://localhost:8080/api/accounts/{accountId}/transactions
```

### Money Movement
```bash
# Deposit
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{"accountId": "UUID", "amount": 10000, "idempotencyKey": "unique-key-1"}'

# Withdraw
curl -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{"accountId": "UUID", "amount": 3000, "idempotencyKey": "unique-key-2"}'

# Transfer
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId": "UUID-A", "toAccountId": "UUID-B", "amount": 5000, "idempotencyKey": "unique-key-3"}'
```

> **Note**: Amounts are in the smallest currency unit (e.g., cents).

## Project Structure
```
src/main/java/com/ledger/engine/
├── api/               # REST Controllers & DTOs
├── domain/            # POJOs (Account, Transaction, LedgerEntry)
├── exception/         # Custom exceptions
├── repository/        # JDBC Repositories
├── service/           # Business logic
└── BankingEngineApplication.java
```

## Documentation
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — System design and data flow
- [INVARIANTS.md](docs/INVARIANTS.md) — Accounting invariants and enforcement
- [API.md](docs/API.md) — Full API reference
- [SEQUENCE_DIAGRAMS.md](docs/SEQUENCE_DIAGRAMS.md) — Operation flow diagrams
- [PROJECT_WALKTHROUGH.md](docs/PROJECT_WALKTHROUGH.md) — Detailed system walkthrough
- [CONTRIBUTING.md](CONTRIBUTING.md) — How to contribute

## License
MIT
