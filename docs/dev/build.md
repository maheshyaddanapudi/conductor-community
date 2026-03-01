# Build & Development Guide

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java | 17 | Enforced via Gradle toolchain; install any JDK 17 distribution (Zulu used in CI) |
| Gradle | 7.6.2 | Bundled via wrapper (`./gradlew`); do not install separately |
| Docker | Any recent | Required for integration tests (Testcontainers spins up MySQL, PostgreSQL, Elasticsearch) |

No other tools are required. The Gradle wrapper handles all build dependencies.

## Common Build Commands

### Full build (compile + test)
```bash
./gradlew build
```
Compiles all modules, runs all tests, and produces artifacts. Requires Docker for Testcontainers-based tests.

### Build without tests
```bash
./gradlew build -x test
```
This is what CI runs on pull requests. Use this for fast compilation checks.

### Build the Spring Boot executable JAR
```bash
./gradlew :community-server:bootJar
```
Produces `community-server/build/libs/*-boot.jar`.

### Clean build artifacts
```bash
./gradlew clean
```

## Running Tests

### Run all tests
```bash
./gradlew test
```

### Run tests for a specific module
```bash
# Module artifact names use the conductor- prefix
./gradlew :persistence:conductor-postgres-persistence:test
./gradlew :persistence:conductor-mysql-persistence:test
./gradlew :index:conductor-es7-persistence:test
./gradlew :event-queue:conductor-amqp:test
./gradlew :task:conductor-kafka:test
./gradlew :lock:conductor-zookeeper-lock:test
./gradlew :conductor-metrics:test
./gradlew :conductor-workflow-event-listener:test
```

### Run a single test class
```bash
./gradlew :persistence:conductor-postgres-persistence:test --tests "*.PostgresExecutionDAOTest"
```

### Run a single test method
```bash
./gradlew :persistence:conductor-postgres-persistence:test --tests "*.PostgresExecutionDAOTest.testTaskExecLog"
```

### Run a Spock specification
```bash
./gradlew :task:conductor-kafka:test --tests "*.KafkaPublishTaskSpec"
```

## Code Formatting

Formatting is enforced by [Spotless](https://github.com/diffplug/spotless) using Google Java Format (AOSP variant).

### Check formatting (does not modify files)
```bash
./gradlew spotlessCheck
```

### Auto-fix formatting violations
```bash
./gradlew spotlessApply
```

Spotless enforces:
- Google Java Format (AOSP style — 4-space indent, no column limit enforcement)
- Import order: `java` → `javax` → `org` → `com.netflix` → (others) → static `com.netflix` → static (others)
- Removal of unused imports
- Apache 2.0 license header on all Java files (from `licenseheader.txt`)

Always run `spotlessApply` before committing. The `spotlessCheck` task runs as part of `build`.

## Code Coverage

```bash
./gradlew jacocoTestReport
```
Generates HTML and XML reports. Output is in each module's `build/reports/jacoco/`.

## Known Gotchas

### Docker must be running for integration tests
The `mysql-persistence`, `postgres-persistence`, `es7-persistence`, `postgres-external-storage`, `kafka`, and `metrics` modules use Testcontainers. Tests will fail if Docker is not available. Use `./gradlew build -x test` to skip tests when Docker is unavailable.

### MySQL and PostgreSQL tests run sequentially
Both `mysql-persistence` and `postgres-persistence` set `maxParallelForks = 1` in their `build.gradle`. Their tests share an embedded database within the same JVM and cannot run in parallel. This makes their test suites slower.

### Flyway migrations run before each test
PostgreSQL and MySQL tests call `flyway.clean()` + `flyway.migrate()` in `@Before` setup. This resets the database schema between tests.

### Shadow JAR in es7-persistence
The `es7-persistence` module uses the Shadow plugin to shade dependencies. The standard `jar` task is replaced by `shadowJar`. If you see classpath issues related to Elasticsearch classes, check that the shadow configuration in `index/es7-persistence/build.gradle` is correct.

### CI skips tests on PRs
The GitHub Actions CI workflow (`ci.yml`) runs `./gradlew build --scan -x test` for non-main branches. Full tests only run on the `main` branch.

### Netflix artifact repositories
The build resolves some dependencies from `artifactory-oss.prod.netflix.net`. If this repository is unreachable, the build may fail on dependency resolution. Core dependencies are available on Maven Central.

### Proxy/network configuration
If running behind a proxy, Java proxy settings may be needed. The Gradle wrapper picks up standard `JAVA_TOOL_OPTIONS` proxy configuration.

## Module Naming in Gradle

Gradle task paths use the `conductor-` prefix for submodule artifacts (applied by `settings.gradle`):

| Directory path | Gradle project path |
|---|---|
| `persistence/mysql-persistence` | `:persistence:conductor-mysql-persistence` |
| `persistence/postgres-persistence` | `:persistence:conductor-postgres-persistence` |
| `persistence/common-persistence` | `:persistence:conductor-common-persistence` |
| `index/es7-persistence` | `:index:conductor-es7-persistence` |
| `event-queue/amqp` | `:event-queue:conductor-amqp` |
| `event-queue/nats` | `:event-queue:conductor-nats` |
| `event-queue/nats-streaming` | `:event-queue:conductor-nats-streaming` |
| `external-payload-storage/azureblob-storage` | `:external-payload-storage:conductor-azureblob-storage` |
| `external-payload-storage/postgres-external-storage` | `:external-payload-storage:conductor-postgres-external-storage` |
| `lock/zookeeper-lock` | `:lock:conductor-zookeeper-lock` |
| `task/kafka` | `:task:conductor-kafka` |
| `metrics` | `:conductor-metrics` |
| `workflow-event-listener` | `:conductor-workflow-event-listener` |
| `test-util` | `:conductor-test-util` |
| `community-server` | `:community-server` |

## Test Framework Details

- **Unit tests**: JUnit 4 (with Vintage engine on JUnit Platform) + Mockito
- **BDD/integration tests**: Spock 2.3 with Groovy 3.0.19
- **Container tests**: Testcontainers 1.18.3 (MySQL 8.0.27, PostgreSQL 11.15-alpine, Elasticsearch 7.x)
- **Test config**: `@TestPropertySource(locations = "classpath:application-integrationtest.properties")` for Spring Boot integration tests
- **Test logging**: Only SKIPPED and FAILED events shown; full exception format enabled
