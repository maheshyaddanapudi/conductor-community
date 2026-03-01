---
paths:
  - '**/db/migration*/**'
  - '**/*.sql'
---

# Flyway Migration Rules

## File Naming

- **ALWAYS** use Flyway versioned naming: `V{N}__{description_with_underscores}.sql`
- **ALWAYS** increment from the current highest version number in the directory:
  - PostgreSQL: current latest is `V8` in `persistence/postgres-persistence/src/main/resources/db/migration_postgres/`
  - MySQL: current latest is `V8` in `persistence/mysql-persistence/src/main/resources/db/migration/`
- **NEVER** use `R__` repeatable migrations in persistence modules — only `external-payload-storage/postgres-external-storage` uses that pattern (`R__initial_schema.sql`)

## Migration Locations

- PostgreSQL persistence: `db/migration_postgres` (configured via `spring.flyway.locations` in test properties)
- MySQL persistence: `db/migration` (Flyway default)
- External PostgreSQL storage: `db/migration_external_postgres`
- **NEVER** mix migration directories between modules — each module has its own isolated migration path

## Safety Rules

- **NEVER** modify an existing migration file — Flyway checksums will fail and the application will not start. Always create a new versioned migration.
- **NEVER** use destructive DDL (`DROP TABLE`, `DROP COLUMN`) without a corresponding data migration step.
- **ALWAYS** test migrations by running the module's test suite — tests call `flyway.clean()` + `flyway.migrate()` in `@Before` setup to validate the full migration chain from scratch.

## Cross-Database Considerations

- **ALWAYS** create separate migration files for PostgreSQL and MySQL if the schema change applies to both — the SQL dialects differ (e.g., `SERIAL` vs `AUTO_INCREMENT`, `BYTEA` vs `BLOB`).
- **NEVER** assume identical migration version numbers between PostgreSQL and MySQL — they track independently.
