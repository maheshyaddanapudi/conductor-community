---
paths:
  - '**/src/test/**/*.java'
  - '**/src/test/**/*.groovy'
---

# Testing Standards Rules

## Test Class Structure (JUnit)

- **ALWAYS** annotate DAO integration tests with `@RunWith(SpringRunner.class)`, `@SpringBootTest`, and `@ContextConfiguration` listing the required configuration classes.
- **ALWAYS** extend `ExecutionDAOTest` (from `common-persistence`) for new persistence DAO tests — it provides shared contract tests (`testTaskExecLog`, `testCreateWorkflow`, etc.).
- **ALWAYS** call `flyway.clean()` then `flyway.migrate()` in `@Before` for persistence tests — this resets the database schema between test runs.

## Test Class Structure (Spock)

- **ALWAYS** extend `AbstractSpecification` (from `test-util`) for Spock integration specs — it provides `@SpringBootTest` context, autowired services, and `cleanup()` that calls `workflowTestUtil.clearWorkflows()`.
- **ALWAYS** use Spock's `given`/`when`/`then`/`and` blocks — not plain method bodies. See `KafkaPublishTaskSpec.groovy` for the pattern.

## Mocking

- **ALWAYS** use Mockito's `mock()` + `when().thenReturn()` in JUnit tests — this is the established pattern in `AMQPEventQueueProviderTest`, `KafkaPublishTaskTest`, etc.
- **ALWAYS** use Spock's `DetachedMockFactory` and `Spy()` in Groovy specs — see `AbstractResiliencySpecification` for the pattern.
- **NEVER** mix Mockito and Spock mocking in the same test file.

## Testcontainers

- **ALWAYS** use Testcontainers JDBC URL format for database tests:
  - PostgreSQL: `jdbc:tc:postgresql:11.15-alpine:///conductor`
  - MySQL: `jdbc:tc:mysql:8.0.27:///conductor`
- **NEVER** start containers manually in test code for databases — the `jdbc:tc:` URL prefix triggers automatic container lifecycle.
- Elasticsearch containers are started statically in test base classes (see `ElasticSearchTest`).

## Naming

- **ALWAYS** name JUnit test classes as `{ClassName}Test.java` or `Test{Feature}.java`.
- **ALWAYS** name Spock specs as `{Feature}Spec.groovy`.
- **ALWAYS** name test methods as `test{Scenario}` in JUnit or `"descriptive sentence"()` in Spock.
