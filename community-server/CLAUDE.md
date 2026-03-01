# Community Server Module

## Overview
Spring Boot application that assembles all community modules into a single runnable server. This is the main entry point for running Conductor with community-contributed backends.

## Main Class
`com.netflix.conductor.Conductor` — Spring Boot application entry point

## Build
```bash
# Build the server (produces bootJar)
./gradlew :conductor-community-server:build

# Build without tests
./gradlew :conductor-community-server:build -x test
```

## Configuration
- Main config: `src/main/resources/application.properties`
- External config via `CONDUCTOR_CONFIG_FILE` system property
- Database selection: `conductor.db.type` property (`memory`, `mysql`, `postgres`)
- Elasticsearch: `conductor.elasticsearch.url` and `conductor.elasticsearch.version`

## Key Dependencies
Pulls in all community modules plus core Conductor modules:
- All persistence backends (MySQL, PostgreSQL)
- All event queues (AMQP, NATS)
- Elasticsearch 7 indexing
- External payload storage (Azure Blob, PostgreSQL)
- Zookeeper lock
- Kafka task
- Metrics and workflow event listener
- Spring Boot Actuator for health/management endpoints
- SpringDoc OpenAPI for API documentation

## Notes
- `DataSourceAutoConfiguration` is excluded by default — re-imported by specific persistence modules
- Produces both a regular JAR and a Spring Boot fat JAR (classifier: `boot`)
- Build info generated via `springBoot { buildInfo() }`
