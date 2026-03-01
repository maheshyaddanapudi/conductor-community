---
paths:
  - '**/dao/**/*.java'
  - '**/persistence/**/*.java'
---

# DAO & Persistence Layer Rules

## Class Structure

- **ALWAYS** extend `PostgresBaseDAO` or `MySQLBaseDAO` when creating new DAO classes — they provide transaction handling (`getWithRetriedTransactions`), query execution (`query`, `execute`), and JSON serialization (`toJson`, `readValue`).
- **NEVER** manage JDBC connections directly — use the inherited `getWithTransaction(TransactionalFunction<R>)` or `withTransaction(Consumer<Connection>)` methods which handle commit/rollback.
- **ALWAYS** inject `RetryTemplate`, `ObjectMapper`, and `DataSource` via the constructor and pass to the super constructor.

## Transaction Safety

- **ALWAYS** use `getWithRetriedTransactions()` for write operations — PostgreSQL can throw serialization failures (SQLState 40001) and deadlocks (40P01) that are handled by the retry template configured in `PostgresConfiguration`.
- **NEVER** set auto-commit on connections — both MySQL and PostgreSQL test configs set `spring.datasource.hikari.auto-commit=false`.

## Logging

- **ALWAYS** initialize the logger as `protected static final Logger logger = LoggerFactory.getLogger(getClass())` — this is the pattern in `PostgresBaseDAO` and `MySQLBaseDAO`.

## Naming

- **ALWAYS** name DAO classes as `{Database}{Domain}DAO` — e.g., `PostgresExecutionDAO`, `MySQLMetadataDAO`, `PostgresQueueDAO`.
- **ALWAYS** place DAO classes in `com.netflix.conductor.{database}.dao` package.
