---
paths:
  - '**/event-queue/**/*.java'
  - '**/queue/**/*.java'
  - '**/task/kafka/**/*.java'
---

# Event Queue & Messaging Rules

## Provider Contract

- **ALWAYS** implement `EventQueueProvider` when adding a new event queue backend — the interface requires `getQueueType()` and `getQueue(String queueURI)`.
- **ALWAYS** use `ConcurrentHashMap.computeIfAbsent()` to cache queue instances in the provider — see `AMQPEventQueueProvider` for the pattern. Queue objects are reused across the application.
- **ALWAYS** return the queue type as a string constant matching the activation property — e.g., `"amqp_queue"`, `"amqp_exchange"`, `"nats"`, `"sqs"`.

## Configuration Pattern

- **ALWAYS** register the provider bean in a `@Configuration` class gated by `@ConditionalOnProperty(name = "conductor.event-queues.{type}.enabled", havingValue = "true")`.
- AMQP registers **two** providers (queue + exchange) — if adding a new backend with multiple modes, follow this dual-registration pattern from `AMQPEventQueueConfiguration`.

## Observable Queue Implementation

- **ALWAYS** extend or implement `ObservableQueue` for the queue adapter.
- **ALWAYS** handle connection failures with retry logic — see `AMQPRetryPattern` and `AMQPConnection` for the recovery pattern with configurable `networkRecoveryIntervalInMilliSecs`.
- **ALWAYS** support message acknowledgement (ack) and negative acknowledgement (nack) — the AMQP module's nack support was added to prevent consumer disconnection during idle periods.

## Kafka Task

- `KafkaPublishTask` is a system task, not an event queue — it publishes messages to Kafka topics as a workflow step.
- `KafkaProducerManager` manages producer lifecycle with a concurrent cache keyed by bootstrap server config.
- **NEVER** create Kafka producers outside `KafkaProducerManager` — it handles producer reuse and cleanup.
