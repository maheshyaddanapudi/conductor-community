# test-util

This supplements the root CLAUDE.md. Read that first.

Shared test infrastructure module. This is NOT a runtime module — it provides base Spock specifications, Groovy test utilities, and end-to-end test scaffolding consumed by other modules as a test dependency.

## Different Technology

This module uses **Groovy 3.0.19** and **Spock 2.3** as `implementation` (not `testImplementation`) dependencies because other modules import its test output. The `groovy` plugin is applied in `build.gradle`.

## Key Files

- `AbstractSpecification.groovy` — base Spock spec with `@SpringBootTest` and `@TestPropertySource("classpath:application-integrationtest.properties")`; provides autowired `ExecutionService`, `MetadataService`, `WorkflowExecutor`; calls `workflowTestUtil.clearWorkflows()` in `cleanup()`
- `AbstractResiliencySpecification.groovy` — extended base spec with `DetachedMockFactory` for creating Spock spies on DAO objects; mocks Redis via `JedisMock`
- `WorkflowTestUtil.groovy` — workflow lifecycle helpers (register definitions, start workflows, poll tasks, clear state)
- `AbstractEndToEndTest.java` — JUnit base for end-to-end workflow tests with template methods (`startWorkflow()`, `getWorkflow()`, `registerTaskDefinitions()`)
- `AbstractGrpcEndToEndTest.java` — extends `AbstractEndToEndTest` with gRPC client setup (`TaskClient`, `WorkflowClient`, `MetadataClient`)
- `ConductorTestApp.java` — minimal `@SpringBootApplication` for test context bootstrapping
- `application-integrationtest.properties` — shared test config (`conductor.db.type=memory`, `conductor.indexing.type=postgres`, disabled system task workers)

## How Other Modules Consume This

Other modules depend on test-util's **test output**, not its main output:
```groovy
testImplementation project(':conductor-test-util').sourceSets.test.output
```
This gives access to `AbstractSpecification`, `AbstractEndToEndTest`, and `WorkflowTestUtil` in their test classpaths.

## Gotchas

- **Do not add runtime code here** — this module exists solely for test infrastructure. The only main source is `ConductorTestApp.java`.
- **Spock specs use `given/when/then` blocks** — follow BDD style when writing new specs. See `KafkaPublishTaskSpec.groovy` in `task/kafka` for an example consumer.
- **Testcontainers for all databases** — this module's test dependencies include Testcontainers for Elasticsearch, MySQL, PostgreSQL, and RabbitMQ. Docker is required.
