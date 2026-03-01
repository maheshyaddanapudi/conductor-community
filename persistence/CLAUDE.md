# Persistence Module

## Overview
Database persistence implementations for Conductor's metadata, execution, and queue data.

## Submodules
- **common-persistence** — Shared base test classes (`ExecutionDAOTest`) and common DAO contracts
- **mysql-persistence** — MySQL implementation using Spring JDBC + Flyway migrations
- **postgres-persistence** — PostgreSQL implementation with optional full-text indexing (alternative to Elasticsearch)

## Key Patterns
- DAO classes extend framework-specific base DAOs (`MySQLBaseDAO`, `PostgresBaseDAO`)
- Database migrations managed by Flyway (migration scripts in `src/main/resources/db/migration/`)
- Tests use Testcontainers — requires Docker to run
- Tests run with `maxParallelForks = 1` (single JVM for shared embedded DB)
- Test classes extend `ExecutionDAOTest` from `common-persistence` for consistent DAO test coverage

## Running Tests
```bash
# All persistence tests (requires Docker for Testcontainers)
./gradlew :persistence:conductor-postgres-persistence:test
./gradlew :persistence:conductor-mysql-persistence:test

# Common persistence tests only
./gradlew :persistence:conductor-common-persistence:test
```

## Important
- PostgreSQL module includes `PostgresIndexDAO` which provides indexing without Elasticsearch
- Both MySQL and PostgreSQL modules depend on `conductor-common-persistence` for shared test base
- Connection configuration is via Spring Boot `application.properties` (datasource settings)
