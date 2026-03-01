# Gradle Build Rules

## Build Commands
- Always use `./gradlew` (wrapper), never a system-installed `gradle`
- Full build: `./gradlew build`
- Build without tests: `./gradlew build -x test`
- Run all tests: `./gradlew test`
- Run module tests: `./gradlew :persistence:conductor-postgres-persistence:test`
- Format code: `./gradlew spotlessApply`
- Check formatting: `./gradlew spotlessCheck`

## Module References
- Submodules use `conductor-` prefix in Gradle project paths
- Example: `:persistence:conductor-mysql-persistence`, `:event-queue:conductor-amqp`
- Top-level modules without children: `:conductor-metrics`, `:conductor-workflow-event-listener`

## Dependency Management
- Spring Boot BOM manages transitive dependency versions
- Version variables are defined in `dependencies.gradle` (e.g., `revConductor`, `revElasticSearch7`)
- Do not add version numbers for dependencies managed by Spring Boot BOM

## Never Run
- `./gradlew snapshot` — publishes to artifact repositories
- `./gradlew publish` — publishes to artifact repositories
- `./gradlew final` — creates a release
- `./gradlew candidate` — creates a release candidate
