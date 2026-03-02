---
name: dependency-impact
description: |
  Analyze the impact of changing a dependency version. Use when user says:
  "what does this dependency affect", "impact of upgrading", "dependency impact",
  "what modules use this library", "upgrade impact", "check dependency"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Dependency Impact Analysis

Analyze which modules are affected when a dependency version changes in `dependencies.gradle`.

## Workflow

1. **Identify the dependency** being changed. Check `dependencies.gradle` for the current version variable name (e.g., `revElasticSearch7`, `revKafka`, `revAmqpClient`).

2. **Search all `build.gradle` files** for references to that version variable:
   ```bash
   grep -r "{variableName}" --include="build.gradle" .
   ```

3. **Search for direct usage** of the dependency artifact in all modules:
   ```bash
   grep -r "artifact-name" --include="build.gradle" .
   ```

4. **Map affected modules** to their Gradle project paths using the module table from `docs/architecture/modules.md`.

5. **Check for transitive impact**:
   - If `conductor-core` version (`revConductor`) changes, ALL modules are affected.
   - If `common-persistence` dependencies change, `mysql-persistence` and `postgres-persistence` are affected.
   - If `test-util` dependencies change, all modules consuming its test output are affected.

6. **Identify test impact** — check if the dependency is `testImplementation` or `implementation`:
   - `testImplementation`: only test code affected
   - `implementation`: runtime code affected — higher risk

7. **Compile check** affected modules:
   ```bash
   ./gradlew {affected-module}:compileJava
   ```

8. **Report**:
   - List of affected modules (direct and transitive)
   - Whether the change is runtime or test-only
   - Compile status for each affected module
   - Reminder: run `./gradlew generateLock saveLock` to refresh lock files after version changes
