# Data Model Documentation

## Overview

Conductor stores data in three logical domains: **Metadata** (workflow/task definitions), **Execution** (runtime state), and **Queue** (internal task queues). All data is stored as JSON blobs in `TEXT`/`mediumtext` columns with relational lookup indexes. Both MySQL and PostgreSQL share the same logical schema (13 core tables). PostgreSQL adds 3 additional index tables for native search support.

## Entity Relationships

```
meta_workflow_def (name, version)  ──────────────────┐
     │                                                │
     │  defines                                       │  type lookup
     ▼                                                ▼
workflow (workflow_id)  ◄──── workflow_def_to_workflow (workflow_def, date_str, workflow_id)
     │
     ├── workflow_pending (workflow_type, workflow_id)     [active tracking]
     │
     ├── workflow_to_task (workflow_id, task_id)           [1:N relationship]
     │        │
     │        ▼
     │   task (task_id, json_data)
     │        │
     │        ├── task_scheduled (workflow_id, task_key)   [dedup tracking]
     │        └── task_in_progress (task_def_name, task_id) [concurrency]
     │
     └── event_execution (event_handler_name, event_name, execution_id)

meta_task_def (name)  ─── defines ──► task_in_progress (task_def_name)

meta_event_handler (name, event)  ─── triggers ──► event_execution

queue (queue_name)  ──── contains ──► queue_message (queue_name, message_id)

poll_data (queue_name, domain)    [task poll metadata]
```

## Core Tables

### Metadata Domain

| Table | Primary Key | Purpose | Key Fields |
|---|---|---|---|
| `meta_workflow_def` | `(name, version)` | Workflow definition versions | `latest_version`, `json_data` (full definition JSON) |
| `meta_task_def` | `(name)` | Task type definitions | `json_data` (concurrency limits, timeouts, retry policy) |
| `meta_event_handler` | `(id)` SERIAL | Event handler definitions | `name`, `event`, `active` (boolean), `json_data` |

### Execution Domain

| Table | Primary Key | Purpose | Key Fields |
|---|---|---|---|
| `workflow` | `(workflow_id)` | Running/completed workflows | `correlation_id`, `json_data` (full workflow state) |
| `task` | `(task_id)` | Task instances | `json_data` (full task state including status) |
| `workflow_to_task` | `(workflow_id, task_id)` | Workflow→task mapping | Join table for 1:N relationship |
| `workflow_pending` | `(workflow_type, workflow_id)` | Active workflow tracking | Indexed by `workflow_type` for type-based queries |
| `workflow_def_to_workflow` | `(workflow_def, date_str, workflow_id)` | Definition→instance mapping | `date_str` enables time-partitioned lookups |
| `task_scheduled` | `(workflow_id, task_key)` | Scheduled task deduplication | Prevents duplicate task scheduling |
| `task_in_progress` | `(task_def_name, task_id)` | In-progress task tracking | `in_progress_status`, `workflow_id` for concurrency limits |
| `event_execution` | `(event_handler_name, event_name, execution_id)` | Event processing records | `message_id` for idempotency |
| `poll_data` | `(queue_name, domain)` | Task poll metadata | `json_data` (last poll time, worker info) |

### Queue Domain

| Table | Primary Key | Purpose | Key Fields |
|---|---|---|---|
| `queue` | `(queue_name)` | Queue registry | Simple existence tracking |
| `queue_message` | `(queue_name, message_id)` | Queued messages | `priority`, `popped` (boolean), `deliver_on`, `offset_time_seconds`, `payload` |

### PostgreSQL Index Tables (V8 migration)

| Table | Primary Key | Purpose | Key Fields |
|---|---|---|---|
| `workflow_index` | `(workflow_id)` | Denormalized workflow search | `correlation_id`, `workflow_type`, `start_time`, `status`, `json_data` (JSONB) |
| `task_index` | `(task_id)` | Denormalized task search | `task_type`, `task_def_name`, `status`, `start_time`, `update_time`, `json_data` (JSONB) |
| `task_execution_logs` | `(log_id)` SERIAL | Task execution log entries | `task_id`, `log` (TEXT), `created_time` |

The index tables use PostgreSQL GIN indexes on `jsonb_to_tsvector()` and `to_tsvector()` for full-text search across JSON payload fields.

## Key Fields and What They Control

### `json_data` Column

All core tables store their full domain object as a JSON blob in `json_data` (TEXT). The relational columns exist only for indexing and querying. This is a **document-store pattern on top of a relational database**.

- `workflow.json_data`: Complete `WorkflowModel` — status, input, output, tasks, variables, event handlers
- `task.json_data`: Complete `TaskModel` — status, input, output, worker ID, poll count, timestamps
- `meta_workflow_def.json_data`: Full `WorkflowDef` — tasks, decision branches, failure workflow
- `meta_task_def.json_data`: Full `TaskDef` — concurrency limit, timeout, retry count/policy

### `queue_message` Fields

| Field | Purpose |
|---|---|
| `priority` | Integer priority (0 = default). Higher values processed first (V4 migration added this). |
| `popped` | Boolean flag — `true` when a worker has taken the message. Used for visibility timeout. |
| `deliver_on` | Timestamp for delayed delivery. Messages with future `deliver_on` are not visible for polling. |
| `offset_time_seconds` | Delay offset applied when message is pushed with a future delivery time. |

## Persistence Patterns

### JSON Document Store

All DAOs serialize/deserialize domain objects via Jackson `ObjectMapper`:
```java
// PostgresBaseDAO / MySQLBaseDAO
protected String toJson(Object value) {
    return objectMapper.writeValueAsString(value);
}
protected <T> T readValue(String json, Class<T> tClass) {
    return objectMapper.readValue(json, tClass);
}
```

### Transaction Safety

**Method:** `getWithRetriedTransactions(TransactionalFunction)` in `PostgresBaseDAO`/`MySQLBaseDAO`

```
1. Acquire Connection from DataSource
2. Set autoCommit = false
3. Execute TransactionalFunction
4. On success: commit
5. On failure: rollback, wrap in NonTransientException
6. Retry via Spring RetryTemplate if deadlock detected
7. Restore autoCommit mode
8. Close Connection (try-with-resources)
```

### Deadlock Retry

| Database | Error Codes | Max Retries | Backoff |
|---|---|---|---|
| PostgreSQL | `40P01` (deadlock), `40001` (serialization failure) | 3 (hardcoded) | None (`NoBackOffPolicy`) |
| MySQL | `1213` (`ER_LOCK_DEADLOCK`) | Configurable via `MySQLProperties` | None |

### Locking Strategies

- **PostgreSQL**: `FOR SHARE` (shared row locks), `FOR UPDATE SKIP LOCKED` (exclusive, skip locked rows)
- **MySQL**: Implicit InnoDB row-level locks (no explicit hints)

## Caching Patterns

### Task Definition Cache

Both `PostgresMetadataDAO` and `MySQLMetadataDAO` maintain an in-memory cache:

```java
ConcurrentHashMap<String, TaskDef> taskDefCache
```

- **Population**: Loaded at startup, refreshed periodically via `ScheduledExecutorService`
- **Refresh interval**: Configurable via `PostgresProperties.getTaskDefCacheRefreshInterval()`
- **Invalidation**: Explicit eviction on `updateTaskDef()` and `removeTaskDef()`
- **Fallback**: Cache miss triggers database query

### Elasticsearch Bulk Indexing Buffer

`ElasticSearchRestDAOV7` uses a `ConcurrentHashMap<String, BulkRequests>` to batch index operations:
- Accumulates requests in-memory
- Flushes when batch size threshold or flush timeout is reached
- Async execution via dedicated `ExecutorService` (pool size: 6)

### No Spring `@Cacheable`

All caching is manual. No Spring cache abstraction is used.

## Migration Strategy

### Flyway Configuration

| Database | Migration Location | Naming | Strategy |
|---|---|---|---|
| PostgreSQL | `classpath:db/migration_postgres` | `V{N}__description.sql` | Versioned, append-only |
| MySQL | `classpath:db/migration` (default) | `V{N}__description.sql` | Versioned, append-only |
| External Storage (PG) | `classpath:db/migration_external_postgres` | `R__initial_schema.sql` | **Repeatable** (re-runs on change) |

### Migration Execution

- PostgreSQL: `Flyway.configure().baselineOnMigrate(true).load()` in `PostgresConfiguration`
- MySQL: Depends on Spring Boot's default Flyway auto-configuration
- Both run before DAO beans are initialized (`@DependsOn` in MySQL)

### Current Migration Versions

Both PostgreSQL and MySQL are at **V8** (8 versioned migrations each):

| Version | MySQL | PostgreSQL |
|---|---|---|
| V1 | Initial 13-table schema | Initial 13-table schema |
| V2 | Queue message timestamps | Fix execution DAO index (message_id→execution_id) |
| V3 | Queue priority column | Correlation ID index |
| V4 | Fix event execution index | Queue message priority index |
| V5 | Correlation ID index | Queue message composite PK |
| V6 | Queue message priority index | Remove auto-increment IDs from all tables |
| V7 | Queue message composite PK | Descending priority index for queue_message |
| V8 | Remove auto-increment IDs from all tables | Index tables (workflow_index, task_index, task_execution_logs) |

### PK Evolution (V6-V8)

All tables originally used `SERIAL`/`AUTO_INCREMENT` surrogate keys. Migrations V6-V8 removed these and switched to **natural composite primary keys** (e.g., `(workflow_id)`, `(queue_name, message_id)`, `(task_def_name, task_id)`). This eliminates redundant `id` columns and matches the actual uniqueness constraints.

## External Payload Storage Schema

The `postgres-external-storage` module uses a separate database with a single table:

```sql
CREATE TABLE ${tableName} (
    id   TEXT PRIMARY KEY,
    data bytea NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- **Storage**: `EXTERNAL` (avoids TOAST compression for binary data)
- **Auto-cleanup trigger**: `keep_row_number_steady()` deletes oldest rows when count exceeds `maxDataRows` or age exceeds configured limit
- **Repeatable migration**: `R__` prefix means it re-runs whenever the SQL changes
