# Flow: Kafka Publish Task

## Overview

The Kafka Publish flow allows workflows to send messages to Kafka topics as a system task. It is the only outbound-only integration in this repository — it produces messages but does not consume them.

## Entry Point

**Class:** `KafkaPublishTask` (`task/kafka/src/main/java/com/netflix/conductor/contribs/tasks/kafka/KafkaPublishTask.java`)
**Method:** `start(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor)`
**Task Type:** `kafka_publish` (registered via `@Component(TASK_TYPE_KAFKA_PUBLISH)`)

## Activation

Always active when the `task/kafka` module is on the classpath. No `@ConditionalOnProperty` gate — the task registers itself as a `WorkflowSystemTask` via `@Component`.

## Flow Trace

```
WorkflowExecutor (conductor-core)
  └─ KafkaPublishTask.start(workflow, task, executor)
       ├─ 1. Extract request from task.getInputData().get("kafka_request")
       ├─ 2. Validate input (4 checks)
       ├─ 3. kafkaPublish(input)
       │     ├─ KafkaProducerManager.getProducer(input)  [cached]
       │     ├─ Serialize key (Long/Integer/String)
       │     ├─ Build ProducerRecord (topic, key, JSON value, headers)
       │     └─ producer.send(record) → Future<RecordMetadata>
       ├─ 4. recordMetaDataFuture.get()  [blocking wait]
       └─ 5. Set task status (COMPLETED / IN_PROGRESS / FAILED)
```

## Validation Logic

Four sequential checks in `start()` (lines 84-104):

1. **Missing request**: `task.getInputData().get("kafka_request")` is null → FAILED
2. **Missing bootstrap servers**: `input.getBootStrapServers()` is blank → FAILED
3. **Missing topic**: `input.getTopic()` is blank → FAILED
4. **Missing value**: `input.getValue()` is null → FAILED

Each validation failure calls `markTaskAsFailed(task, reason)` and returns immediately.

## Input Model

**Class:** `KafkaPublishTask.Input` (inner class, lines 211-311)

| Field | Type | Default | Required |
|---|---|---|---|
| `bootStrapServers` | `String` | none | Yes |
| `topic` | `String` | none | Yes |
| `key` | `Object` | none | No |
| `value` | `Object` | none | Yes |
| `keySerializer` | `String` | `StringSerializer` | No |
| `requestTimeoutMs` | `Integer` | 100ms (from manager) | No |
| `maxBlockMs` | `Integer` | 500ms (from manager) | No |
| `headers` | `Map<String, Object>` | empty map | No |

## Producer Management

**Class:** `KafkaProducerManager` (`task/kafka/src/main/java/com/netflix/conductor/contribs/tasks/kafka/KafkaProducerManager.java`)

- **Caching**: Guava `Cache<Properties, Producer>` with:
  - Max size: 10 producers (configurable via `@Value`)
  - Expiry: 120 seconds after last access
  - `RemovalListener` calls `producer.close()` on eviction
- **Producer properties built from input**:
  - `BOOTSTRAP_SERVERS_CONFIG` = `input.getBootStrapServers()`
  - `KEY_SERIALIZER_CLASS_CONFIG` = `input.getKeySerializer()` (default `StringSerializer`)
  - `VALUE_SERIALIZER_CLASS_CONFIG` = `StringSerializer` (always)
  - `REQUEST_TIMEOUT_MS_CONFIG` = from input or default 100ms
  - `MAX_BLOCK_MS_CONFIG` = from input or default 500ms

## Key Serialization

**Method:** `getKey(Input input)` (lines 183-194)

- `LongSerializer` → `Long.parseLong(key)`
- `IntegerSerializer` → `Integer.parseInt(key)`
- Default (`StringSerializer`) → `String.valueOf(key)`

## Error Handling

| Error | Handling | Task Status |
|---|---|---|
| Null request / missing fields | `markTaskAsFailed()` with descriptive message | `FAILED` |
| `ExecutionException` from Kafka | Logged, task marked failed with cause message | `FAILED` |
| Any other `Exception` | Logged with input details, task marked failed | `FAILED` |
| Successful send | Blocks on `future.get()`, sets status | `COMPLETED` or `IN_PROGRESS` (async) |

## Async Completion

If `isAsyncComplete(task)` returns true (configured in task definition), the task is set to `IN_PROGRESS` after successful send, and completion is handled externally. Otherwise, it is immediately `COMPLETED`.

## Cross-Module Interactions

- **conductor-core**: `WorkflowSystemTask` base class, `WorkflowExecutor` orchestration, `TaskModel`/`WorkflowModel` domain objects
- **No persistence interaction**: This task does not read from or write to any DAO
- **No event emission**: The task does not emit internal Conductor events
- **Metrics**: No explicit metrics instrumentation in this flow

## Cancellation

**Method:** `cancel(workflow, task, executor)` — sets `task.setStatus(TaskModel.Status.CANCELED)`. Does not cancel in-flight Kafka sends.
