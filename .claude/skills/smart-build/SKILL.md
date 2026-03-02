---
name: smart-build
description: |
  Build only affected modules based on changed files. Use when user says:
  "build my changes", "fast build", "incremental build", "build what I changed",
  "build affected modules", "quick build", "smart build"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Smart Build

Build only the Gradle modules affected by current changes instead of the full project.

## Workflow

1. **Detect changed files**:
   ```bash
   git diff --name-only HEAD
   git diff --name-only --cached
   ```

2. **Map files to Gradle modules** using this lookup:

   | Path prefix | Gradle project |
   |---|---|
   | `persistence/mysql-persistence/` | `:persistence:conductor-mysql-persistence` |
   | `persistence/postgres-persistence/` | `:persistence:conductor-postgres-persistence` |
   | `persistence/common-persistence/` | `:persistence:conductor-common-persistence` |
   | `index/es7-persistence/` | `:index:conductor-es7-persistence` |
   | `event-queue/amqp/` | `:event-queue:conductor-amqp` |
   | `event-queue/nats/` | `:event-queue:conductor-nats` |
   | `event-queue/nats-streaming/` | `:event-queue:conductor-nats-streaming` |
   | `external-payload-storage/azureblob-storage/` | `:external-payload-storage:conductor-azureblob-storage` |
   | `external-payload-storage/postgres-external-storage/` | `:external-payload-storage:conductor-postgres-external-storage` |
   | `lock/zookeeper-lock/` | `:lock:conductor-zookeeper-lock` |
   | `task/kafka/` | `:task:conductor-kafka` |
   | `metrics/` | `:conductor-metrics` |
   | `workflow-event-listener/` | `:conductor-workflow-event-listener` |
   | `test-util/` | `:conductor-test-util` |
   | `community-server/` | `:community-server` |
   | `dependencies.gradle` or `build.gradle` (root) | Full build required |

3. **If `dependencies.gradle` or root `build.gradle` changed**, run full build:
   ```bash
   ./gradlew build -x test
   ```

4. **Otherwise**, build only affected modules:
   ```bash
   ./gradlew {module1}:build -x test {module2}:build -x test
   ```

5. **If `common-persistence` changed**, also build `mysql-persistence` and `postgres-persistence` (they depend on it).

6. **Report** which modules were built and whether the build succeeded.
