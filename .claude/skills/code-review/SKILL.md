---
name: code-review
description: |
  Review code changes against project conventions. Use when user says:
  "review my code", "code review", "review changes", "check my PR",
  "review this", "look at my changes", "pre-review check"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Code Review

Review staged or unstaged changes against project conventions and patterns.

## Workflow

1. **Gather changes**:
   ```bash
   git diff HEAD
   git diff --cached
   git diff --stat HEAD
   ```

2. **Check formatting compliance**:
   ```bash
   ./gradlew spotlessCheck
   ```

3. **Review each changed file** against these criteria:

   ### Java Source Files
   - License header present (Apache 2.0 from `licenseheader.txt`)
   - Import order: `java` → `javax` → `org` → `com.netflix` → others → static
   - `@Configuration` classes have `@ConditionalOnProperty` gating
   - `@Configuration` classes use `proxyBeanMethods = false`
   - DAO classes extend the appropriate `BaseDAO` (`PostgresBaseDAO`, `MySQLBaseDAO`, `ElasticSearchBaseDAO`)
   - Properties classes use `@ConfigurationProperties` with correct prefix
   - No `@Autowired` field injection — constructor injection only
   - No manual JDBC connection management — use BaseDAO helper methods

   ### Test Files
   - Test naming: `{Class}Test.java` or `Test{Feature}.java` (JUnit), `{Feature}Spec.groovy` (Spock)
   - DAO tests extend `ExecutionDAOTest` from `common-persistence`
   - Spock specs extend `AbstractSpecification` from `test-util`
   - Mockito and Spock mocking not mixed in same file
   - Testcontainers JDBC URLs use `jdbc:tc:` prefix

   ### SQL Migration Files
   - Versioned naming `V{N}__{description}.sql`
   - No modification of existing migrations
   - Next version number is correct

   ### Build Files
   - Version variables referenced from `dependencies.gradle`, not hardcoded
   - `dependencies.lock` files not manually edited

4. **Check for common issues**:
   - Unused imports (Spotless removes these, but verify)
   - Missing null checks on external API boundaries
   - Thread safety in cached/shared objects (especially in `EventQueueProvider` implementations)

5. **Report** findings organized by severity:
   - **Blocking**: Build will fail or conventions violated
   - **Warning**: Potential issues that should be addressed
   - **Suggestion**: Improvements that could be made
