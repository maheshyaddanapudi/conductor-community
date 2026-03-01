# Module Reference

## community-server

- **Path**: `community-server/`
- **Responsibility**: Spring Boot application that assembles all community modules into a single executable JAR.
- **Entry point**: `com.netflix.conductor.Conductor` — `@SpringBootApplication` main class that loads config from `CONDUCTOR_CONFIG_FILE` system property.
- **Config**: `src/main/resources/application.properties` — default runtime configuration with `conductor.db.type=memory` and `conductor.indexing.enabled=false`.
- **Dependencies**: All other community modules (as project dependencies), plus external Conductor libraries (`conductor-rest`, `conductor-core`, `conductor-grpc-server`, `conductor-redis-persistence`, `conductor-cassandra-persistence`).
- **Depended on by**: Nothing (this is the top-level runnable artifact).
- **Build note**: Applies `org.springframework.boot` plugin; produces `*-boot.jar` via `bootJar` task.

## persistence/common-persistence

- **Path**: `persistence/common-persistence/`
- **Artifact**: `conductor-common-persistence`
- **Responsibility**: Shared DAO abstractions and utilities for SQL-based persistence modules.
- **Key classes**: Provides base test class `ExecutionDAOTest` (in `src/test/`) that MySQL and PostgreSQL modules extend.
- **Dependencies**: `conductor-common`, `conductor-core`, Jackson, Commons Lang3.
- **Depended on by**: `mysql-persistence`, `postgres-persistence` (compile + test output).

## persistence/mysql-persistence

- **Path**: `persistence/mysql-persistence/`
- **Artifact**: `conductor-mysql-persistence`
- **Responsibility**: MySQL-backed persistence for metadata, execution state, and queue DAOs.
- **Key classes**: `MySQLExecutionDAO`, `MySQLMetadataDAO`, `MySQLQueueDAO` in `com.netflix.conductor.mysql.dao`; `MySQLConfiguration` in `com.netflix.conductor.mysql.config`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.db.type", havingValue = "mysql")`
- **Dependencies**: MySQL Connector 8.0.33, Flyway (MySQL), Spring JDBC, `common-persistence`.
- **Depended on by**: `community-server`.
- **Tests**: Testcontainers MySQL 8.0.27; `maxParallelForks = 1` (sequential test execution required).
- **Package structure**: `.config` (Spring config), `.dao` (DAO implementations).

## persistence/postgres-persistence

- **Path**: `persistence/postgres-persistence/`
- **Artifact**: `conductor-postgres-persistence`
- **Responsibility**: PostgreSQL-backed persistence for metadata, execution state, queue, and index DAOs.
- **Key classes**: `PostgresExecutionDAO`, `PostgresMetadataDAO`, `PostgresQueueDAO`, `PostgresIndexDAO` in `com.netflix.conductor.postgres.dao`; `PostgresConfiguration` in `com.netflix.conductor.postgres.config`; `PostgresIndexQueryBuilder` in `com.netflix.conductor.postgres.util`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.db.type", havingValue = "postgres")`
- **Dependencies**: PostgreSQL Driver 42.3.8, Flyway Core, Spring JDBC, `common-persistence`.
- **Depended on by**: `community-server`.
- **Tests**: Testcontainers PostgreSQL 11.15-alpine; `maxParallelForks = 1`.
- **Package structure**: `.config`, `.dao`, `.util`.

## index/es7-persistence

- **Path**: `index/es7-persistence/`
- **Artifact**: `conductor-es7-persistence`
- **Responsibility**: Elasticsearch 7 indexing for workflow and task search.
- **Key classes**: `ElasticSearchRestDAOV7` in `com.netflix.conductor.es7.dao.index`; `ElasticSearchV7Configuration` in `com.netflix.conductor.es7.config`; query parser classes in `com.netflix.conductor.es7.dao.query.parser`.
- **Activation**: Custom `@Conditional(ElasticSearchConditions.ElasticSearchV7Enabled.class)`
- **Dependencies**: ES REST client, ES REST high-level client 7.17.13, Commons IO, Guava.
- **Depended on by**: `community-server`.
- **Build note**: Uses Shadow JAR plugin (7.0.0) for dependency shading; replaces standard JAR artifact.
- **Package structure**: `.config`, `.dao.index`, `.dao.query.parser`, `.dao.query.parser.internal`.

## event-queue/amqp

- **Path**: `event-queue/amqp/`
- **Artifact**: `conductor-amqp`
- **Responsibility**: RabbitMQ/AMQP event queue integration implementing `EventQueueProvider`.
- **Key classes**: `AMQPObservableQueue` (queue implementation), `AMQPEventQueueProvider` (provider), `AMQPEventQueueConfiguration` (Spring config), `AMQPEventQueueProperties` (properties binding) in `com.netflix.conductor.contribs.queue.amqp`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.event-queues.amqp.enabled", havingValue = "true")`
- **Dependencies**: RabbitMQ AMQP Client 5.13.0, RxJava, Guava.
- **Depended on by**: `community-server`.
- **Package structure**: `.config`, root package for queue/provider classes.

## event-queue/nats

- **Path**: `event-queue/nats/`
- **Artifact**: `conductor-nats`
- **Responsibility**: NATS core event queue integration implementing `EventQueueProvider`.
- **Key classes**: `NATSObservableQueue`, `NATSEventQueueProvider`, `NATSEventQueueConfiguration`, `NATSEventQueueProperties` in `com.netflix.conductor.contribs.queue.nats`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.event-queues.nats.enabled", havingValue = "true")`
- **Dependencies**: NATS Java Client (jnats) 2.15.6, RxJava, Guava.
- **Depended on by**: `community-server`.

## event-queue/nats-streaming

- **Path**: `event-queue/nats-streaming/`
- **Artifact**: `conductor-nats-streaming`
- **Responsibility**: NATS Streaming event queue integration implementing `EventQueueProvider`.
- **Key classes**: `NATSStreamObservableQueue`, `NATSStreamEventQueueProvider`, `NATSStreamEventQueueConfiguration`, `NATSStreamEventQueueProperties` in `com.netflix.conductor.contribs.queue.stan`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.event-queues.nats-stream.enabled", havingValue = "true")`
- **Dependencies**: NATS Streaming Client 2.6.5, NATS Core (jnats) 2.15.6, RxJava, Guava.
- **Depended on by**: `community-server`.

## external-payload-storage/azureblob-storage

- **Path**: `external-payload-storage/azureblob-storage/`
- **Artifact**: `conductor-azureblob-storage`
- **Responsibility**: Azure Blob Storage backend for large workflow payloads implementing `ExternalPayloadStorage`.
- **Key classes**: `AzureBlobPayloadStorage` in `com.netflix.conductor.azureblob.storage`; `AzureBlobConfiguration`, `AzureBlobProperties` in `com.netflix.conductor.azureblob.config`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.external-payload-storage.type", havingValue = "azureblob")`
- **Dependencies**: Azure Storage Blob SDK 12.7.0.
- **Depended on by**: `community-server`.

## external-payload-storage/postgres-external-storage

- **Path**: `external-payload-storage/postgres-external-storage/`
- **Artifact**: `conductor-postgres-external-storage`
- **Responsibility**: PostgreSQL-backed external payload storage with a REST endpoint for retrieval.
- **Key classes**: `PostgresPayloadStorage` in `com.netflix.conductor.postgres.storage`; `ExternalPostgresPayloadResource` (REST controller at `/api/external/postgres`) in `com.netflix.conductor.postgres.controller`; `PostgresPayloadConfiguration`, `PostgresPayloadProperties` in `com.netflix.conductor.postgres.config`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.external-payload-storage.type", havingValue = "postgres")`
- **Dependencies**: PostgreSQL Driver, Flyway Core, Spring Web, SpringDoc OpenAPI.
- **Depended on by**: `community-server`.
- **Tests**: Testcontainers PostgreSQL.

## lock/zookeeper-lock

- **Path**: `lock/zookeeper-lock/`
- **Artifact**: `conductor-zookeeper-lock`
- **Responsibility**: ZooKeeper-based distributed lock for workflow execution.
- **Key classes**: `ZookeeperLock` in `com.netflix.conductor.zookeeper.lock`; `ZookeeperConfiguration`, `ZookeeperProperties` in `com.netflix.conductor.zookeeper.config`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.workflow-execution-lock.type", havingValue = "zookeeper")`
- **Dependencies**: Apache Curator Recipes 5.4.0.
- **Depended on by**: `community-server`.
- **Tests**: Curator Test framework (embedded ZooKeeper).

## metrics

- **Path**: `metrics/`
- **Artifact**: `conductor-metrics`
- **Responsibility**: Metrics collection and export via Prometheus, Datadog, or logging.
- **Key classes**: `MetricsRegistryConfiguration`, `PrometheusMetricsConfiguration`, `DatadogMetricsConfiguration`, `LoggingMetricsConfiguration` in `com.netflix.conductor.contribs.metrics`.
- **Activation**: Multiple `@ConditionalOnProperty` conditions per exporter (`conductor.metrics-prometheus.enabled`, `conductor.metrics-datadog.enabled`, `conductor.metrics-logger.enabled`).
- **Dependencies**: Netflix Spectator 0.122.0, Micrometer 1.6.2, Prometheus simpleclient 0.9.0.
- **Depended on by**: `community-server`.

## task/kafka

- **Path**: `task/kafka/`
- **Artifact**: `conductor-kafka`
- **Responsibility**: Kafka Publish system task for sending messages to Kafka topics from workflows.
- **Key classes**: `KafkaPublishTask` (task implementation), `KafkaProducerManager` (producer lifecycle) in `com.netflix.conductor.contribs.tasks.kafka`; `KafkaPublishTaskMapper` in `com.netflix.conductor.core.execution.mapper`; `KafkaPublishTaskConfiguration` in `com.netflix.conductor.contribs.tasks.kafka.config`.
- **Dependencies**: Kafka Clients 2.6.0, RxJava, Guava.
- **Depended on by**: `community-server`.
- **Tests**: Spock specs (`KafkaPublishTaskSpec.groovy`), Testcontainers MockServer.

## workflow-event-listener

- **Path**: `workflow-event-listener/`
- **Artifact**: `conductor-workflow-event-listener`
- **Responsibility**: Listens for workflow status changes and archives completed/terminated workflows.
- **Key classes**: `ArchivingWorkflowStatusListener` in `com.netflix.conductor.contribs.listener.archive`; `ArchivingWorkflowListenerConfiguration`, `ArchivingWorkflowListenerProperties` in `com.netflix.conductor.contribs.listener.archive`.
- **Activation**: `@ConditionalOnProperty(name = "conductor.workflow-status-listener.type", havingValue = "archive")`
- **Dependencies**: `conductor-common`, `conductor-core`.
- **Depended on by**: `community-server`.
- **Tests**: Spock specs and JUnit with `@SpringBootTest`.

## test-util

- **Path**: `test-util/`
- **Artifact**: `conductor-test-util`
- **Responsibility**: Shared test infrastructure including base Spock specifications, workflow test utilities, and Testcontainers setup for integration tests.
- **Key classes**: `AbstractSpecification` (base Spock spec with Spring Boot context), `AbstractResiliencySpecification` (resilience testing base), `WorkflowTestUtil` (workflow lifecycle helpers) in `com.netflix.conductor.test.base` / `com.netflix.conductor.test.util`; `AbstractEndToEndTest`, `AbstractGrpcEndToEndTest` in `com.netflix.conductor.test.integration`.
- **Dependencies**: Conductor server/client/gRPC, Spock, Groovy, Testcontainers (ES, MySQL, PostgreSQL).
- **Depended on by**: `mysql-persistence`, `postgres-persistence`, `postgres-external-storage`, `kafka`, `workflow-event-listener` (all as test dependencies).
