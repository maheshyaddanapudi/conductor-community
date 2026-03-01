---
paths:
  - '**/config/**/*.java'
  - '**/*Configuration.java'
  - '**/*Properties.java'
---

# Spring Configuration Rules

## Configuration Classes

- **ALWAYS** annotate with `@Configuration(proxyBeanMethods = false)` — every configuration class in this repo disables proxy bean methods for performance.
- **ALWAYS** add `@ConditionalOnProperty` to gate activation — each module activates on a specific property:
  - Persistence: `conductor.db.type` = `postgres` | `mysql`
  - Event queues: `conductor.event-queues.{type}.enabled` = `true`
  - Storage: `conductor.external-payload-storage.type` = `azureblob` | `postgres`
  - Lock: `conductor.workflow-execution-lock.type` = `zookeeper`
  - Metrics: `conductor.metrics-{type}.enabled` = `true`
  - Listener: `conductor.workflow-status-listener.type` = `archive`
- **ALWAYS** pair with `@EnableConfigurationProperties(XxxProperties.class)` when the module has configurable properties.
- **NEVER** import `DataSourceAutoConfiguration` in a module config unless the module manages its own datasource — only `PostgresConfiguration` and `MySQLConfiguration` do this via `@Import(DataSourceAutoConfiguration.class)`.

## Properties Classes

- **ALWAYS** annotate with `@ConfigurationProperties("conductor.{feature-path}")`.
- **ALWAYS** use private fields with JavaBean getters/setters — Spring Boot binds these from `application.properties`.
- **ALWAYS** provide sensible defaults inline (e.g., `private int batchSize = 1`).

## Bean Registration

- **ALWAYS** use `@DependsOn({"flywayForPrimaryDb"})` for DAO beans in persistence modules — ensures Flyway migrations complete before DAOs are initialized. See `PostgresConfiguration` for the pattern.
- **ALWAYS** use constructor injection — no `@Autowired` field injection in configuration classes.
