# Ledger-Based Banking Engine

A high-performance, double-entry bookkeeping engine built with Spring Boot and PostgreSQL. It ensures strong consistency, idempotency, and auditability for all financial transactions.

## 🚀 Quick Start (Local Demo)

Calculates balances on-the-fly from immutable ledger entries.

### Prerequisites
- Docker (for the database)
- Java 17+ (or use the included Maven wrapper)

### Run Locally
1. Start the database:
   ```bash
   docker run -d --name ledger-pg \
     -e POSTGRES_DB=banking_engine \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5433:5432 \
     postgres:15-alpine
   ```

2. Run the application:
   ```bash
   # Connects to the DB on port 5433 and serves the app on port 8080
   DB_HOST=localhost DB_PORT=5433 DB_NAME=banking_engine DB_USER=postgres DB_PASSWORD=postgres SERVER_PORT=8080 mvn spring-boot:run
   ```

3. Open the Demo App:
   👉 **[http://localhost:8080/demo-app/index.html](http://localhost:8080/demo-app/index.html)**

---

## 🔌 Integrating Limitless Possibilities

This engine is designed to be the core of any banking ecosystem.

### Connect Any Frontend (CORS Enabled)
The API is fully open to cross-origin requests (`CORS` allowed for `*`), meaning you can build your frontend in:
- **React / Vue / Angular** (running on localhost:3000, etc.)
- **Mobile Apps** (iOS / Android)
- **Serverless Functions**

**API Base URL**: `http://localhost:8080/api`

#### Endpoints
- `POST /api/accounts` - Create a new account
- `GET /api/accounts/{id}/balance` - Get current balance
- `POST /api/transactions/deposit` - Deposit funds
- `POST /api/transactions/withdraw` - Withdraw funds
- `POST /api/transactions/transfer` - Transfer between accounts

### Connect Any Database
The application is configured via environment variables, allowing you to connect to any PostgreSQL instance (local, AWS RDS, Google Cloud SQL, Azure Database, etc.) without code changes.

**Configuration:**
| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `DB_HOST` | `localhost` | Database hostname |
| `DB_PORT` | `5432` | Database port |
| `DB_NAME` | `banking_engine` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `DB_POOL_SIZE` | `10` | Max connection pool size |

**Note**: To use a different database type (e.g., MySQL, Oracle), add the corresponding JDBC driver dependency to `pom.xml` and update `spring.datasource.driver-class-name` in `application.properties`.

---

## 🏗 Architecture
- **Language**: Java 17+ (Spring Boot 3.2.5)
- **Database**: PostgreSQL (Spring Data JDBC)
- **Migration**: Flyway
- **Testing**: Testcontainers (Integration tests)

### Core Principles
1. **Double-Entry Ledger**: Every transaction records a credit and a debit.
2. **Immutability**: Ledger entries are never updated or deleted.
3. **Derived Balances**: Account balance is the sum of all ledger entries.
4. **Idempotency**: Requests have unique keys to prevent duplicate processing.
