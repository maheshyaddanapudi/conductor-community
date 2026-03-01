# Java Conventions for Conductor Community

## Code Style
- Use Google Java Format (AOSP variant) — enforced by Spotless
- Run `./gradlew spotlessApply` before committing any Java changes
- Import order: `java`, `javax`, `org`, `com.netflix`, blank, static `com.netflix`, static wildcard
- All new files must include the Apache 2.0 license header (auto-applied by Spotless)

## Java Version
- Target Java 17 — do not use features from newer Java versions

## Spring Boot Patterns
- Use `@Configuration(proxyBeanMethods = false)` for config classes
- Use `@ConfigurationProperties` for externalized configuration
- Use `@ConditionalOnProperty` for feature toggles
- Avoid `DataSourceAutoConfiguration` in modules that don't need JDBC

## Package Naming
- Root namespace: `com.netflix.conductor`
- Config classes: `com.netflix.conductor.<module>.config`
- DAO classes: `com.netflix.conductor.<module>.dao`
- Queue implementations: `com.netflix.conductor.contribs.queue.<type>`
- Task implementations: `com.netflix.conductor.contribs.tasks.<type>`
- Listener implementations: `com.netflix.conductor.contribs.listener`

## Testing
- Name test classes with `*Test.java` suffix
- Place tests in `src/test/java/` mirroring the main source tree
- Use JUnit 4/5 with JUnit Vintage Engine for backward compatibility
- Use Mockito for unit tests, Spring Boot Test for integration tests
- Use Testcontainers for tests requiring external services (databases, message brokers)
- Use Flyway `clean()` + `migrate()` in `@Before` for database test isolation

## Logging
- Use SLF4J API (`org.slf4j.Logger`, `LoggerFactory`)
- Runtime logging is Log4j2 (logback is excluded globally)
- Never use `System.out.println` — always use the logger

## Dependencies
- Prefer Spring Boot BOM-managed versions where available
- Core Conductor dependencies use version variable `revConductor`
- Declare version variables in `dependencies.gradle`
