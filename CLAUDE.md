# Netflix Conductor Community Modules

## Project Overview
Community-contributed modules and extensions for [Netflix Conductor](https://github.com/Netflix/conductor) — a workflow orchestration engine. This repository provides pluggable implementations for persistence, queuing, indexing, metrics, locking, and external storage that integrate with the core Conductor framework.

- **Language**: Java 17
- **Build System**: Gradle (wrapper included)
- **Framework**: Spring Boot 2.7.x
- **Group ID**: `com.netflix.conductor`
- **Core Conductor Version**: 3.15.0
- **License**: Apache 2.0

## Quick Reference

### Build Commands
```bash
# Full build (compile + test)
./gradlew build

# Build without tests
./gradlew build -x test

# Run tests only
./gradlew test

# Run tests for a specific module
./gradlew :persistence:conductor-postgres-persistence:test

# Format code (Google Java Format, AOSP variant)
./gradlew spotlessApply

# Check formatting without fixing
./gradlew spotlessCheck

# Generate test coverage report
./gradlew jacocoTestReport
```

### Code Style
- **Formatter**: Google Java Format (AOSP variant) enforced via Spotless plugin
- **Import order**: `java`, `javax`, `org`, `com.netflix`, everything else, then static `com.netflix` imports, then all other static
- **License header**: Auto-applied from `licenseheader.txt` (Apache 2.0)
- **Always run** `./gradlew spotlessApply` before committing

### Testing
- **Framework**: JUnit 4/5 via JUnit Vintage Engine + Spring Boot Test
- **Test naming**: `*Test.java` suffix convention
- **Test location**: `src/test/java/` mirroring main source tree
- **Pattern**: Uses `@SpringBootTest`, Testcontainers for integration tests, Mockito for unit tests
- **CI runs**: `./gradlew build --scan` (tests included on main branch)

## Module Architecture

### Parent Modules (aggregate only, no source)
| Module | Purpose |
|--------|---------|
| `event-queue/` | Event queue implementations |
| `external-payload-storage/` | External payload storage backends |
| `persistence/` | Data persistence implementations |
| `index/` | Search indexing backends |
| `lock/` | Distributed lock implementations |
| `task/` | System task implementations |

### Leaf Modules (contain source code)
| Module | Artifact | Description |
|--------|----------|-------------|
| `event-queue/amqp` | `conductor-amqp` | AMQP/RabbitMQ event queue |
| `event-queue/nats` | `conductor-nats` | NATS JetStream event queue |
| `event-queue/nats-streaming` | `conductor-nats-streaming` | NATS Streaming event queue |
| `external-payload-storage/azureblob-storage` | `conductor-azureblob-storage` | Azure Blob Storage |
| `external-payload-storage/postgres-external-storage` | `conductor-postgres-external-storage` | PostgreSQL payload storage |
| `index/es7-persistence` | `conductor-es7-persistence` | Elasticsearch 7 indexing |
| `lock/zookeeper-lock` | `conductor-zookeeper-lock` | ZooKeeper distributed lock |
| `metrics/` | `conductor-metrics` | Prometheus & logging metrics |
| `persistence/common-persistence` | `conductor-common-persistence` | Shared persistence test base |
| `persistence/mysql-persistence` | `conductor-mysql-persistence` | MySQL persistence |
| `persistence/postgres-persistence` | `conductor-postgres-persistence` | PostgreSQL persistence |
| `task/kafka` | `conductor-kafka` | Kafka publish task |
| `workflow-event-listener/` | `conductor-workflow-event-listener` | Workflow status listener |
| `community-server/` | `conductor-community-server` | Assembled server with all modules |
| `test-util/` | `conductor-test-util` | End-to-end test utilities |

### Package Convention
All source code lives under `com.netflix.conductor.*`:
- Configs: `com.netflix.conductor.<module>.config.*`
- DAOs: `com.netflix.conductor.<module>.dao.*`
- Queue implementations: `com.netflix.conductor.contribs.queue.<type>.*`
- Task implementations: `com.netflix.conductor.contribs.tasks.<type>.*`
- Listeners: `com.netflix.conductor.contribs.listener.*`

## Key Dependencies
- **Spring Boot 2.7.x** (BOM for dependency management)
- **Netflix Conductor Core** (`conductor-common`, `conductor-core`) at version `3.15.0`
- **Jackson** for JSON serialization
- **Log4j2** for logging (logback excluded globally)
- **Testcontainers** for integration tests requiring databases/services

## Important Notes
- The `settings.gradle` renames all child module artifacts with a `conductor-` prefix
- Javadoc generation is disabled for all subprojects
- The `community-server` module uses Spring Boot plugin and produces a bootJar
- PR target branch is `main`
