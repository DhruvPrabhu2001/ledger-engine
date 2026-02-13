# Ledger Engine
A simple ledger engine built to demonstrate financial correctness for transactions, by utilizing immutable entries, double entry accounting and concurrency control.

## Why Ledger Engine?

Building financial systems is hard because mistakes are expensive. I built this engine to handle the heavy lifting of financial integrity at bare minimum, and to learn how financial systems work.

- **Truth in History**: Your balance isn't just a number in a database, it's the sum of every transaction that ever happened. This simple engine should make auditing a simpler task than usual..
- **Rock Solid Reliability**: By implementing pessimistic locking and strict transaction ordering, it ensures that two people can't spend the same dollar at the same time, and it should sustain even in high concurrency environments.
- **Fail Safe Processing**: With the help of idempotency keys, if some error were to occur and a client retries a request, the system ensures the transaction only happens once.

## Getting Started

The quickest way to see the engine in action is using Docker.

1. **Spin up the environment**:
   ```bash
   docker-compose up -d
   ```
2. **Access the API**:
   The engine will be available at `http://localhost:8080`. You can start creating accounts and performing transfers immediately.
