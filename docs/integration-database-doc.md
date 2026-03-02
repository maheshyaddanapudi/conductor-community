# Integration: Relational Databases (PostgreSQL / MySQL)

## Summary

| Aspect | PostgreSQL | MySQL |
|---|---|---|
| **Protocol** | JDBC | JDBC |
| **Driver** | PostgreSQL 42.3.8 | MySQL Connector 8.0.33 |
| **Module** | `persistence/postgres-persistence` | `persistence/mysql-persistence` |
| **Direction** | Bidirectional | Bidirectional |
| **Activation** | `conductor.db.type=postgres` | `conductor.db.type=mysql` |
| **Index Support** | Built-in (`PostgresIndexDAO`) | None (requires Elasticsearch) |

## Architecture

Both databases implement the same four DAO interfaces from `conductor-core`:

| Interface | PostgreSQL DAO | MySQL DAO |
|---|---|---|
| `ExecutionDAO` | `PostgresExecutionDAO` | `MySQLExecutionDAO` |
| `MetadataDAO` | `PostgresMetadataDAO` | `MySQLMetadataDAO` |
| `QueueDAO` | `PostgresQueueDAO` | `MySQLQueueDAO` |
| `IndexDAO` | `PostgresIndexDAO` | Not implemented |

Shared abstractions live in `persistence/common-persistence`:
- `PostgresBaseDAO` / `MySQLBaseDAO` — transaction management, JSON serialization, retry logic
- `Query` utility class — prepared statement wrapper with fluent parameter binding
- `ExecutorsUtil` — named thread factory for scheduled executors

## Data Flow

### Write Path

```
Conductor Core Service Layer
  └─ ExecutionDAO.createWorkflow(WorkflowModel)
       └─ PostgresExecutionDAO / MySQLExecutionDAO
            └─ PostgresBaseDAO.withTransaction(connection -> {
                    execute(conn, INSERT_WORKFLOW_SQL, q -> {
                        q.addParameter(workflowId)
                         .addParameter(correlationId)
                         .addParameter(toJson(workflow))
                         .executeUpdate();
                    });
                    execute(conn, INSERT_PENDING_SQL, ...);
                    execute(conn, INSERT_DEF_TO_WORKFLOW_SQL, ...);
               })
```

### Read Path

```
Conductor REST API
  └─ ExecutionDAO.getWorkflowById(workflowId)
       └─ queryWithTransaction(SELECT_WORKFLOW_SQL, q -> {
               q.addParameter(workflowId);
               return q.executeAndFetchFirst(rs ->
                   readValue(rs.getString("json_data"), WorkflowModel.class)
               );
           })
```

## Connection Lifecycle

### DataSource

Both modules use Spring Boot's `DataSourceAutoConfiguration` (imported explicitly in their `@Configuration` classes). The connection pool is managed by Spring Boot defaults (HikariCP).

### Transaction Pattern

All database operations go through `PostgresBaseDAO.getWithRetriedTransactions()`:

```java
<R> R getWithRetriedTransactions(TransactionalFunction<R> function) {
    return retryTemplate.execute(context -> getWithTransaction(function));
}

private <R> R getWithTransaction(TransactionalFunction<R> function) {
    try (Connection tx = dataSource.getConnection()) {
        tx.setAutoCommit(false);
        try {
            R result = function.apply(tx);
            tx.commit();
            return result;
        } catch (Throwable th) {
            tx.rollback();
            throw new NonTransientException(th.getMessage(), th);
        } finally {
            tx.setAutoCommit(previousAutoCommitMode);
        }
    }
}
```

### Flyway Migration

| Database | Flyway Location | Bean |
|---|---|---|
| PostgreSQL | `classpath:db/migration_postgres` | `flywayForPrimaryDb()` with `@Bean(initMethod = "migrate")` |
| MySQL | `classpath:db/migration` | Spring Boot default + `@DependsOn({"flyway", "flywayInitializer"})` |

Migrations run before DAO beans initialize.

## Retry / Error Handling

### Deadlock Detection

Both databases use a `CustomRetryPolicy` that extends `SimpleRetryPolicy`:

| Database | Retried Error Codes | Max Retries | Backoff |
|---|---|---|---|
| PostgreSQL | `40P01` (deadlock), `40001` (serialization) | 3 (hardcoded) | None |
| MySQL | `1213` (`ER_LOCK_DEADLOCK`) | Configurable | None |

The `CustomRetryPolicy.canRetry()` method inspects the exception chain for SQL error codes. Only deadlock-related errors trigger retries; all other errors are propagated immediately as `NonTransientException`.

### Error Propagation

All exceptions are wrapped in `NonTransientException` (from `conductor-core`):
- `JsonProcessingException` → `NonTransientException` (serialization failure)
- `SQLException` → `NonTransientException` (connection/query failure)
- Any `Throwable` in transaction → rollback + `NonTransientException`

### Locking Strategies

**PostgreSQL:**
- `FOR SHARE` — shared row locks for reads that must see consistent data
- `FOR UPDATE SKIP LOCKED` — exclusive locks for task polling; skips locked rows to avoid contention

**MySQL:**
- No explicit lock hints; relies on InnoDB's default row-level locking

## Configuration

### PostgreSQL Properties (`PostgresProperties`)

| Property | Description |
|---|---|
| `conductor.db.type` | Must be `postgres` |
| `conductor.postgres.schema` | Database schema name |
| `conductor.postgres.taskDefCacheRefreshInterval` | Task definition cache refresh interval |
| Standard Spring DataSource properties | `spring.datasource.url`, etc. |

### MySQL Properties (`MySQLProperties`)

| Property | Description |
|---|---|
| `conductor.db.type` | Must be `mysql` |
| `conductor.mysql.deadlockRetryMax` | Max deadlock retry attempts |
| `conductor.mysql.taskDefCacheRefreshInterval` | Task definition cache refresh interval |
| Standard Spring DataSource properties | `spring.datasource.url`, etc. |

## PostgreSQL vs MySQL Differences

| Feature | PostgreSQL | MySQL |
|---|---|---|
| **Search/Index** | Built-in `PostgresIndexDAO` with JSONB + GIN indexes | Requires external Elasticsearch |
| **Text column** | `TEXT` | `mediumtext` |
| **Auto-increment** | `SERIAL` | `int(11) unsigned AUTO_INCREMENT` |
| **Locking** | Explicit `FOR SHARE` / `FOR UPDATE SKIP LOCKED` | Implicit InnoDB locks |
| **Executor shutdown** | `@PreDestroy` with 30s graceful shutdown | No graceful shutdown |
| **Retry config** | Hardcoded 3 retries | Configurable via properties |
| **Migration location** | `db/migration_postgres` (non-default) | `db/migration` (Flyway default) |
| **Date column type** | `varchar(60)` for `date_str` | `integer` for `date_str` |

## Failure Behavior

| Failure | Behavior |
|---|---|
| Database unreachable | `SQLException` → `NonTransientException` → 503 to caller |
| Deadlock | Retry up to N times; then `NonTransientException` |
| Constraint violation | `NonTransientException` (no retry) |
| Serialization error | `NonTransientException` (no retry) |
| Connection pool exhausted | Blocks until connection available (HikariCP default) |
