---
name: feature-scaffold
description: |
  Scaffold a new community module with correct structure. Use when user says:
  "create new module", "add new module", "scaffold module", "new persistence module",
  "new event queue", "new storage backend", "feature scaffold", "new integration"
allowed-tools:
  - Bash
  - Read
  - Write
  - Glob
  - Grep
---

# Feature Scaffold

Create a new community module following the established project conventions.

## Workflow

1. **Determine module type** — ask the user which category:
   - Persistence (`persistence/{name}`) — implements `ExecutionDAO`, `MetadataDAO`, `QueueDAO`
   - Event Queue (`event-queue/{name}`) — implements `EventQueueProvider` + `ObservableQueue`
   - External Storage (`external-payload-storage/{name}`) — implements `ExternalPayloadStorage`
   - Lock (`lock/{name}`) — implements distributed lock
   - Task (`task/{name}`) — implements system task
   - Other — custom module

2. **Create directory structure**:
   ```
   {parent}/{module-name}/
   ├── build.gradle
   └── src/
       ├── main/
       │   ├── java/com/netflix/conductor/{package}/
       │   │   ├── config/
       │   │   │   ├── {Name}Configuration.java
       │   │   │   └── {Name}Properties.java
       │   │   └── {implementation classes}
       │   └── resources/
       └── test/
           ├── java/com/netflix/conductor/{package}/
           └── resources/
   ```

3. **Create `build.gradle`** following the pattern of existing modules:
   - Add `conductor-common` and `conductor-core` as implementation dependencies
   - Add Spring Boot starter as `compileOnly`
   - Reference versions from `dependencies.gradle`

4. **Create Configuration class** with:
   - `@Configuration(proxyBeanMethods = false)`
   - `@ConditionalOnProperty(name = "conductor.{feature}.type", havingValue = "{name}")`
   - `@EnableConfigurationProperties({Name}Properties.class)`

5. **Create Properties class** with:
   - `@ConfigurationProperties("conductor.{feature-path}")`

6. **Register in `settings.gradle`**:
   - Add `include '{parent}:{module-name}'` entry

7. **Add to `community-server/build.gradle`**:
   - Add `implementation project(':{parent}:conductor-{module-name}')` dependency

8. **Add license header** to all Java files (from `licenseheader.txt`).

9. **Run** `./gradlew spotlessApply` to format new files.

10. **Verify** with `./gradlew build -x test`.
