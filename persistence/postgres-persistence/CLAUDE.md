# persistence/postgres-persistence

This supplements the root CLAUDE.md. Read that first.

PostgreSQL persistence module. Implements `ExecutionDAO`, `MetadataDAO`, `QueueDAO`, and `IndexDAO` for storing workflow state in PostgreSQL. This is the only persistence module that also implements `IndexDAO` (MySQL does not).

## Flyway Migrations

Migration files live in `src/main/resources/db/migration_postgres/` (note the `_postgres` suffix — configured in test `application.properties` via `spring.flyway.locations`).

- Versioned migrations follow `V{N}__{description}.sql` naming (e.g., `V8__indexing.sql`)
- **NEVER modify existing migrations** — always append new ones with the next version number
- Current latest: `V8__indexing.sql`
- Tests call `flyway.clean()` + `flyway.migrate()` in `@Before` to reset schema between runs

## Key Files

- `PostgresConfiguration.java` — Spring config with custom retry policy for deadlock/serialization failures (`@ConditionalOnProperty(name = "conductor.db.type", havingValue = "postgres")`)
- `PostgresBaseDAO.java` — shared base with JDBC helper methods (`query()`, `getWithRetriedTransactions()`)
- `PostgresIndexDAO.java` — unique to this module (not in MySQL); provides SQL-based workflow search
- `PostgresIndexQueryBuilder.java` — builds parameterized SQL from Conductor query syntax
- `ExecutorsUtil.java` — manages `ScheduledExecutorService` lifecycle with shutdown handlers

## Test Setup

- Testcontainers JDBC URL: `jdbc:tc:postgresql:11.15-alpine:///conductor`
- `maxParallelForks = 1` — tests share an embedded PostgreSQL and must run sequentially
- Tests extend `ExecutionDAOTest` from `common-persistence` (shared DAO contract tests)
- `PostgresGrpcEndToEndTest` extends `AbstractGrpcEndToEndTest` from `test-util`

## Differences from MySQL Module

- Has `PostgresIndexDAO` and `PostgresIndexQueryBuilder` (MySQL has no index DAO)
- Uses `spring.flyway.locations=classpath:db/migration_postgres` (MySQL uses default `db/migration`)
- Has `ExecutorsUtil` for scheduled executor cleanup (MySQL does not)
