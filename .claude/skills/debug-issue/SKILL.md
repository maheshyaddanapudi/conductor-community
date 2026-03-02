---
name: debug-issue
description: |
  Investigate and diagnose a bug or issue. Use when user says:
  "debug this", "why is this failing", "investigate issue", "find the bug",
  "help me debug", "what's wrong with", "troubleshoot", "diagnose"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Debug Issue

Systematically investigate and diagnose a bug or failure in the codebase.

## Workflow

1. **Gather context** — ask the user for:
   - Error message or stack trace
   - Which module is affected
   - Steps to reproduce (if known)
   - Whether it's a build failure, test failure, or runtime issue

2. **Classify the issue**:

   ### Build Failure
   - Check `./gradlew build -x test` output
   - Common causes: missing dependency in `dependencies.gradle`, incompatible version bump, Spotless formatting failure
   - If `dependencies.lock` related: run `./gradlew generateLock saveLock`

   ### Test Failure
   - Run the specific failing test:
     ```bash
     ./gradlew :{module}:test --tests "*.{TestClass}" --info
     ```
   - Check if Docker is running (Testcontainers tests fail silently without Docker)
   - For persistence tests: check if `flyway.clean()` + `flyway.migrate()` sequence is executing in `@Before`
   - For ES tests: check Elasticsearch container startup logs and `awaitility` timeouts

   ### Runtime Issue
   - Check `community-server/src/main/resources/application.properties` for misconfigured properties
   - Verify `@ConditionalOnProperty` activation — the module might not be loading
   - Check `conductor.db.type`, `conductor.indexing.enabled`, `conductor.event-queues.*.enabled`

3. **Trace the code path**:
   - Identify the entry point class (Configuration → Bean → DAO/Provider/Task)
   - Read the relevant source files
   - Check for `@ConditionalOnProperty` mismatches that could prevent bean registration

4. **Check git history** for recent changes to affected files:
   ```bash
   git log --oneline -10 -- {affected-file}
   ```

5. **Identify root cause** and propose a fix with:
   - Which file(s) need changes
   - The specific code modification
   - Which tests to run to verify the fix
   - Whether the fix requires a Flyway migration (for schema issues)

6. **Verify the fix**:
   ```bash
   ./gradlew :{module}:test --tests "*.{TestClass}"
   ./gradlew spotlessCheck
   ```
