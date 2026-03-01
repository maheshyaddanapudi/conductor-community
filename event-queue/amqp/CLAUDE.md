# event-queue/amqp

This supplements the root CLAUDE.md. Read that first.

RabbitMQ/AMQP event queue module. Implements `EventQueueProvider` and `ObservableQueue` for Conductor's event-driven workflows. This is the most complex event-queue module with the largest configuration surface.

## Configuration Complexity

`AMQPEventQueueProperties` exposes 20+ properties under `conductor.event-queues.amqp.*` including connection settings, retry behavior, queue/exchange options, SSL, and channel management. Key activation property: `conductor.event-queues.amqp.enabled=true`.

`AMQPEventQueueConfiguration` registers two `EventQueueProvider` beans — one for queues and one for exchanges — controlled by `conductor.event-queues.amqp.useExchange`.

## Key Files

- `AMQPObservableQueue.java` — core queue implementation (highest-churn file, 8 commits); handles publish, subscribe, ack/nack, connection recovery
- `AMQPEventQueueProvider.java` — implements `EventQueueProvider`, parses queue URIs to construct `AMQPObservableQueue` instances
- `AMQPConnection.java` — manages RabbitMQ `Connection` lifecycle with retry and recovery
- `AMQPSettings.java` — parses AMQP URI strings into queue/exchange configuration
- `AMQPRetryPattern.java` — retry logic with configurable `RetryType` (regular intervals or exponential backoff)
- `AMQPConfigurations.java` / `AMQPConstants.java` — shared configuration and constant definitions

## Patterns Unique to This Module

- **URI parsing**: Queue names encode configuration as structured URIs (e.g., `amqp_exchange:myExchange?exchangeType=topic&routingKey=foo`). See `AMQPSettings` for the parsing logic.
- **Dual provider registration**: `AMQPEventQueueConfiguration` registers separate providers for `amqp_queue` and `amqp_exchange` queue types, unlike NATS modules which register a single provider.
- **Nack support**: `AMQPObservableQueue` supports message nack (negative acknowledgement) to avoid consumer disconnection during idle periods. This was added to fix Issue#3408.
- **Multiple queue binding**: Supports binding multiple queues to the same exchange via the `AMQPEventQueueProvider`.

## Gotchas

- **Connection recovery**: `AMQPConnection` uses automatic recovery with configurable `networkRecoveryIntervalInMilliSecs`. Tests mock the `ConnectionFactory` — do not attempt real RabbitMQ connections in unit tests.
- **No Testcontainers in this module**: Unlike persistence modules, AMQP tests use pure Mockito mocks. Testcontainers RabbitMQ is only in `test-util`.
