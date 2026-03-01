# Repository Map

## What This Repository Is

This is the **conductor-community** repository — a collection of community-contributed plugin modules for [Netflix Conductor](https://github.com/Netflix/conductor), a workflow orchestration engine. It provides pluggable implementations for persistence (MySQL, PostgreSQL), indexing (Elasticsearch 7), event queues (AMQP/RabbitMQ, NATS, NATS Streaming), external payload storage (Azure Blob, PostgreSQL), distributed locking (ZooKeeper), metrics (Prometheus, Datadog), and task integrations (Kafka). The `community-server` module assembles all plugins into a single Spring Boot executable JAR. Netflix discontinued maintenance of Conductor OSS in December 2023.

## Technology Stack

| Component | Version |
|---|---|
| Java | 17 (toolchain) |
| Gradle | 7.6.2 (wrapper) |
| Spring Boot | 2.7.16 |
| Conductor Core | 3.15.0 |
| Groovy | 3.0.19 (test specs) |
| Spock | 2.3-groovy-3.0 |
| Testcontainers | 1.18.3 |
| Elasticsearch | 7.17.13 |
| Code formatting | Spotless (Google Java Format, AOSP) |

## Major Domains

| Domain | Modules | Purpose |
|---|---|---|
| **Persistence** | `persistence/common-persistence`, `persistence/mysql-persistence`, `persistence/postgres-persistence` | Store workflow metadata, execution state, and task queues in relational databases |
| **Indexing** | `index/es7-persistence` | Index workflows and tasks for search via Elasticsearch 7 REST API |
| **Event Queues** | `event-queue/amqp`, `event-queue/nats`, `event-queue/nats-streaming` | Integrate external messaging systems as Conductor event queues |
| **External Storage** | `external-payload-storage/azureblob-storage`, `external-payload-storage/postgres-external-storage` | Offload large workflow input/output payloads to external storage |
| **Locking** | `lock/zookeeper-lock` | Distributed workflow execution locks via ZooKeeper |
| **Metrics** | `metrics` | Expose runtime metrics via Prometheus, Datadog, or logging |
| **Tasks** | `task/kafka` | System task implementations (Kafka Publish) |
| **Workflow Listener** | `workflow-event-listener` | Listen to workflow status changes for archiving |
| **Server** | `community-server` | Spring Boot application assembling all modules |
| **Test Utilities** | `test-util` | Shared test infrastructure (base specs, Testcontainers setup) |

## Navigation Guide

| If you need to... | Look in... |
|---|---|
| Add a new database backend | `persistence/` — create a new submodule following `postgres-persistence` as a template |
| Change MySQL/PostgreSQL schema | `persistence/*/src/main/resources/db/migration*` (Flyway migrations) |
| Fix an Elasticsearch indexing bug | `index/es7-persistence/src/main/java/com/netflix/conductor/es7/dao/` |
| Add a new event queue provider | `event-queue/` — implement `EventQueueProvider` interface |
| Modify AMQP/RabbitMQ integration | `event-queue/amqp/src/main/java/com/netflix/conductor/contribs/queue/amqp/` |
| Change Kafka task behavior | `task/kafka/src/main/java/com/netflix/conductor/contribs/tasks/kafka/` |
| Add a new external storage backend | `external-payload-storage/` — implement `ExternalPayloadStorage` interface |
| Modify server startup or config | `community-server/src/main/java/com/netflix/conductor/Conductor.java` |
| Change default config properties | `community-server/src/main/resources/application.properties` |
| Modify metrics collection | `metrics/src/main/java/com/netflix/conductor/contribs/metrics/` |
| Add or modify test infrastructure | `test-util/` |
| Change dependency versions | `dependencies.gradle` (centralized version catalog) |
| Add a new module | `settings.gradle` (module registration) and `community-server/build.gradle` (add dependency) |

## Module Dependency Flow

```
community-server (Spring Boot app)
├── conductor-core, conductor-rest, conductor-grpc-server  [external: Netflix Conductor]
├── conductor-redis-persistence, conductor-cassandra-persistence  [external]
├── persistence/common-persistence
│   ├── persistence/mysql-persistence
│   └── persistence/postgres-persistence
├── index/es7-persistence
├── event-queue/amqp
├── event-queue/nats
├── event-queue/nats-streaming
├── external-payload-storage/azureblob-storage
├── external-payload-storage/postgres-external-storage
├── lock/zookeeper-lock
├── metrics
├── task/kafka
└── workflow-event-listener

test-util (shared test infra)
├── Used by: mysql-persistence, postgres-persistence, postgres-external-storage,
│            kafka, workflow-event-listener (as test dependency)
└── Depends on: conductor-core, conductor-server, conductor-client [external]
```

All community modules depend on `conductor-core` and `conductor-common` from the external Netflix Conductor project. No community module depends on another community module at runtime (they are all independent plugins), except that `mysql-persistence` and `postgres-persistence` share code from `common-persistence`.
