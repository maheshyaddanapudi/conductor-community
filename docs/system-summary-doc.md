# System Architecture Summary

## Runtime Purpose

Conductor-community is a **plugin assembly** for Netflix Conductor, a workflow orchestration engine. It does not implement the core workflow engine — that comes from the external `conductor-core` dependency. This repository provides pluggable backend implementations for persistence, indexing, event queues, external payload storage, distributed locking, metrics, and task integrations. The `community-server` module assembles everything into a single Spring Boot executable JAR.

## Major Components and Responsibilities

| Component | Module | Responsibility |
|---|---|---|
| **Entry Point** | `community-server` | `Conductor.java` — Spring Boot main class; loads config, bootstraps context |
| **Relational Persistence** | `persistence/mysql-persistence`, `persistence/postgres-persistence` | Store workflow metadata, execution state, task queues in MySQL or PostgreSQL |
| **Shared DAO Abstractions** | `persistence/common-persistence` | Base classes and test infrastructure shared by MySQL and PostgreSQL |
| **Search Indexing** | `index/es7-persistence` | Index workflows/tasks for search via Elasticsearch 7 REST API |
| **AMQP Event Queue** | `event-queue/amqp` | RabbitMQ-backed event queue (`EventQueueProvider` + `ObservableQueue`) |
| **NATS Event Queue** | `event-queue/nats` | NATS core + JetStream event queues |
| **NATS Streaming Queue** | `event-queue/nats-streaming` | Legacy NATS Streaming event queue |
| **Kafka Task** | `task/kafka` | `KafkaPublishTask` — system task that publishes messages to Kafka topics |
| **Azure Blob Storage** | `external-payload-storage/azureblob-storage` | Offload large payloads to Azure Blob Storage |
| **PostgreSQL Payload Storage** | `external-payload-storage/postgres-external-storage` | Offload large payloads to PostgreSQL with REST retrieval endpoint |
| **ZooKeeper Locking** | `lock/zookeeper-lock` | Distributed workflow execution locks via Apache Curator |
| **Metrics** | `metrics` | Bridge Netflix Spectator metrics to Prometheus, Datadog, or logging |
| **Workflow Listener** | `workflow-event-listener` | Archive completed/terminated workflows (immediate or TTL-delayed) |
| **Test Infrastructure** | `test-util` | Shared Spock/JUnit base classes and Testcontainers setup |

## Bootstrap Mechanism

### Entry Point: `com.netflix.conductor.Conductor`

**File:** `community-server/src/main/java/com/netflix/conductor/Conductor.java`

```
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

1. Reads external config file path from system property `CONDUCTOR_CONFIG_FILE`
2. Loads properties from that file and sets them as system properties (overriding defaults)
3. Calls `SpringApplication.run(Conductor.class, args)`
4. Spring Boot scans all modules on the classpath for `@Configuration` classes
5. Each module's configuration is gated by `@ConditionalOnProperty` — only the selected backends activate
6. `DataSourceAutoConfiguration` is explicitly excluded; each persistence module manages its own datasource

### Configuration Priority

System properties > `CONDUCTOR_CONFIG_FILE` > `application.properties` > Spring Boot defaults

### Conditional Activation Map

| Property | Values | Activates |
|---|---|---|
| `conductor.db.type` | `mysql`, `postgres`, `memory` | `MySQLConfiguration` or `PostgresConfiguration` |
| `conductor.indexing.type` | `postgres` | `PostgresIndexDAO` (within `PostgresConfiguration`) |
| `conductor.event-queues.amqp.enabled` | `true` | `AMQPEventQueueConfiguration` |
| `conductor.event-queues.nats.enabled` | `true` | `NATSConfiguration` (core NATS) |
| `conductor.event-queues.jsm.enabled` | `true` | `JetStreamConfiguration` (NATS JetStream) |
| `conductor.event-queues.nats-stream.enabled` | `true` | `NATSStreamConfiguration` (NATS Streaming) |
| `conductor.default-event-queue.type` | `amqp`, `jsm`, `nats_stream` | Default queues for workflow status events |
| `conductor.external-payload-storage.type` | `azureblob`, `postgres` | `AzureBlobConfiguration` or `PostgresPayloadConfiguration` |
| `conductor.workflow-execution-lock.type` | `zookeeper`, `noop_lock` | `ZookeeperLockConfiguration` |
| `conductor.workflow-status-listener.type` | `archive`, `queue_publisher` | `ArchivingWorkflowListenerConfiguration` or `ConductorQueueStatusPublisherConfiguration` |
| `conductor.metrics-prometheus.enabled` | `true` | `PrometheusMetricsConfiguration` |
| `conductor.metrics-logger.enabled` | `true` | `LoggingMetricsConfiguration` + `MetricsRegistryConfiguration` |
| ES7 custom conditional | `ElasticSearchConditions.ElasticSearchV7Enabled` | `ElasticSearchV7Configuration` |

## Integration Points with External Systems

| System | Protocol | Module | Direction |
|---|---|---|---|
| **MySQL** | JDBC | `mysql-persistence` | Bidirectional (read/write) |
| **PostgreSQL** | JDBC | `postgres-persistence`, `postgres-external-storage` | Bidirectional |
| **Elasticsearch 7** | REST HTTP | `es7-persistence` | Bidirectional (index/search) |
| **RabbitMQ** | AMQP 0-9-1 | `event-queue/amqp` | Bidirectional (publish/subscribe) |
| **NATS** | NATS protocol | `event-queue/nats` | Bidirectional |
| **NATS Streaming** | NATS Streaming protocol | `event-queue/nats-streaming` | Bidirectional |
| **Kafka** | Kafka producer API | `task/kafka` | Outbound only |
| **Azure Blob Storage** | HTTPS (Azure SDK) | `azureblob-storage` | Bidirectional (upload/download) |
| **ZooKeeper** | ZooKeeper protocol | `zookeeper-lock` | Bidirectional (acquire/release locks) |
| **Prometheus** | HTTP scrape endpoint | `metrics` | Outbound (metrics exposition) |
| **Datadog** | Datadog agent protocol | `metrics` | Outbound |

## Configuration Surface

### Primary Config File

**File:** `community-server/src/main/resources/application.properties`

Key defaults:
- `conductor.db.type=memory` — in-memory storage (no external DB)
- `conductor.indexing.enabled=false` — indexing disabled
- `conductor.app.workflow-execution-lock-enabled=false` — no distributed locking
- `conductor.workflow-execution-lock.type=noop_lock` — no-op lock
- `conductor.default-event-queue.type=sqs` — AWS SQS (from external conductor-core)

All AMQP, NATS, metrics, and storage properties are commented out by default.

### Environment Variables and Feature Flags

- `CONDUCTOR_CONFIG_FILE` — system property pointing to external config file
- No Spring profiles are used; all feature activation is via `@ConditionalOnProperty`
- No feature flag framework; properties act as feature flags

## High-Level Execution Model

**Hybrid: Request-driven + Event-driven**

1. **Request-driven**: Core Conductor REST APIs (from `conductor-rest`) handle workflow definitions, task polling, and workflow execution via HTTP. The only community-contributed REST endpoint is `GET /api/external/postgres/{path}` for PostgreSQL payload retrieval.

2. **Event-driven**: Event queue modules (AMQP, NATS, Kafka) use RxJava `Observable` patterns with interval-based polling or push-based subscriptions. Messages are consumed via `ObservableQueue.observe()` which returns an `Observable<Message>`.

3. **Listener callbacks**: `WorkflowStatusListener` implementations receive synchronous callbacks on workflow completion/termination for archiving.

4. **Scheduled (internal)**: `ArchivingWithTTLWorkflowStatusListener` uses `ScheduledThreadPoolExecutor` for delayed workflow archival — not Spring `@Scheduled`.

5. **No `@Scheduled` tasks**: This repository contains no Spring-managed scheduled tasks.

6. **No message listener annotations**: No `@RabbitListener`, `@KafkaListener`, or `@EventListener` — all message handling is through Conductor's `ObservableQueue` abstraction.

## Runtime Initialization Sequence

```
JVM Start
  └─ Conductor.main()
       ├─ Load CONDUCTOR_CONFIG_FILE (if set)
       ├─ Set system properties
       └─ SpringApplication.run()
            ├─ Component scan all modules
            ├─ Evaluate @ConditionalOnProperty gates
            ├─ Create beans for selected backends only
            ├─ Run Flyway migrations (if MySQL/PostgreSQL selected)
            ├─ Initialize event queue connections (if enabled)
            ├─ Start embedded Tomcat (Spring Web)
            ├─ Expose REST APIs (conductor-rest + community endpoints)
            └─ Conductor workflow engine ready
```
