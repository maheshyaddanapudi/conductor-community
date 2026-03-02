---
name: test-coverage
description: |
  Run tests and analyze coverage for changed modules. Use when user says:
  "check coverage", "test coverage", "run tests with coverage", "coverage report",
  "what's my coverage", "how much is covered", "test my changes"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Test Coverage Analysis

Run tests and generate coverage reports for affected modules.

## Workflow

1. **Identify affected modules** from changed files:
   ```bash
   git diff --name-only HEAD
   ```
   Map changed paths to Gradle module names (see `CLAUDE.md` architecture table).

2. **Check Docker availability** (required for Testcontainers):
   ```bash
   docker info > /dev/null 2>&1 && echo "Docker OK" || echo "Docker not available — integration tests will fail"
   ```

3. **Run tests for affected modules**:
   ```bash
   ./gradlew :{module}:test
   ```
   Note: `mysql-persistence` and `postgres-persistence` use `maxParallelForks = 1` and will be slower.

4. **Generate JaCoCo coverage report**:
   ```bash
   ./gradlew :{module}:jacocoTestReport
   ```

5. **Read coverage report** from `{module}/build/reports/jacoco/test/jacocoTestReport.xml` or the HTML report at `{module}/build/reports/jacoco/test/html/index.html`.

6. **Analyze coverage gaps**:
   - Identify classes with no test coverage
   - Identify methods in changed files that lack test coverage
   - Check if new public methods have corresponding test methods

7. **Check test existence** for new code:
   - New DAO classes should have tests extending `ExecutionDAOTest` (from `common-persistence`)
   - New Configuration classes should have at least a Spring context loading test
   - New event queue providers should test `getQueue()` and `getQueueType()`

8. **Report**:
   - Per-module test results (pass/fail count)
   - Coverage percentage for changed files
   - Specific untested methods or branches
   - Suggestions for missing tests
