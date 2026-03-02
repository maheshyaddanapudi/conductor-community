# Flow: Event Queue Message Processing

## Overview

Event queue modules (AMQP, NATS, NATS Streaming) implement Conductor's `EventQueueProvider`/`ObservableQueue` contracts to bridge external messaging systems into Conductor's event-driven workflow triggers. Messages flow bidirectionally: Conductor publishes events to queues and subscribes to queues for workflow triggers.

## Entry Points

### Provider Registration

Each module registers one or more `EventQueueProvider` beans:

| Module | Provider Class | Activation Property |
|---|---|---|
| `event-queue/amqp` | `AMQPEventQueueProvider` | `conductor.event-queues.amqp.enabled=true` |
| `event-queue/amqp` | (exchange variant, same config) | same |
| `event-queue/nats` | `NATSEventQueueProvider` | `conductor.event-queues.nats.enabled=true` |
| `event-queue/nats` | `JetStreamEventQueueProvider` | `conductor.event-queues.jsm.enabled=true` |
| `event-queue/nats-streaming` | `NATSStreamEventQueueProvider` | `conductor.event-queues.nats-stream.enabled=true` |
| `event-queue/nats-streaming` | `NATSEventQueueProvider` (stan variant) | `conductor.event-queues.nats.enabled=true` |

### Queue Creation

`EventQueueProvider.getQueue(String queueURI)` → parses URI, returns cached or new `ObservableQueue`

### Subscription

`ObservableQueue.observe()` → returns `rx.Observable<Message>` that emits messages

## AMQP Flow (Most Complex)

**File:** `event-queue/amqp/src/main/java/com/netflix/conductor/contribs/queue/amqp/AMQPObservableQueue.java`

### URI Parsing

**Class:** `AMQPSettings` (`event-queue/amqp/src/main/java/com/netflix/conductor/contribs/queue/amqp/util/AMQPSettings.java`)

Format: `amqp_queue:myQueue?durable=true&exclusive=false` or `amqp_exchange:myExchange?exchangeType=topic&routingKey=key`

Regex: `^(?:amqp_(queue|exchange))?:?(?<name>[^?]+)?(?<params>.*)$`

Supported parameters: `exchangeType`, `bindQueueName`, `routingKey`, `durable`, `exclusive`, `autoDelete`, `deliveryMode`, `x-max-priority`

### Connection Architecture

**Class:** `AMQPConnection` (`event-queue/amqp/src/main/java/com/netflix/conductor/contribs/queue/amqp/AMQPConnection.java`)

- **Singleton** per factory+addresses combination
- **Dual connections**: Separate `publisherConnection` and `subscriberConnection`
- **Channel pool**: `ConcurrentHashMap<ConnectionType, Set<Channel>>` with borrow/return pattern
- **Auto-recovery**: `factory.setAutomaticRecoveryEnabled(true)` + `setTopologyRecoveryEnabled(true)`
- **Retry**: Configurable retry count and duration via `AMQPRetryPattern`

### Message Flow

```
Conductor Core EventProcessor
  └─ AMQPEventQueueProvider.getQueue(queueURI)
       ├─ Parse URI via AMQPSettings
       ├─ Check ConcurrentHashMap cache
       └─ Create AMQPObservableQueue (if not cached)
            ├─ AMQPConnection.getInstance() → singleton
            └─ observe()
                 ├─ Sequential mode: Observable.interval(100ms) → getMessages()
                 │    └─ channel.basicGet() per iteration
                 └─ Event mode: DefaultConsumer.handleDelivery()
                      └─ Push messages to subscriber
```

### Publishing

```
AMQPObservableQueue.publish(List<Message>)
  └─ For each message:
       ├─ AMQPConnection.borrowChannel(PUBLISHER)
       ├─ Build BasicProperties (contentType, deliveryMode, etc.)
       ├─ channel.basicPublish(exchange, routingKey, props, body)
       └─ AMQPConnection.returnChannel()
```

### Acknowledgment

- **ACK**: `channel.basicAck(deliveryTag, false)` with retry loop
- **NACK**: `channel.basicNack(deliveryTag, false, false)` with retry loop

## NATS JetStream Flow

**File:** `event-queue/nats/src/main/java/com/netflix/conductor/contribs/queue/nats/JetStreamObservableQueue.java`

```
JetStreamEventQueueProvider.getQueue(queueURI)
  └─ Create JetStreamObservableQueue
       ├─ Nats.connectAsynchronously() with ConnectionListener
       ├─ JetStreamManagement.addStream() (WorkQueue retention, configurable storage)
       └─ observe()
            └─ Observable.interval(pollTimeDuration)
                 └─ Drain LinkedBlockingQueue<Message>
                      └─ Populated by JetStream.subscribe() push callback
```

- **Durable consumers**: `PushSubscribeOptions.builder().durable(name).build()`
- **Manual ACK**: Messages wrapped in `JsmMessage` with `msg.ack()`
- **Reconnection**: `ConnectionListener` re-subscribes on `CONNECTED`/`RECONNECTED` events

## NATS Core Flow

**File:** `event-queue/nats/src/main/java/com/netflix/conductor/contribs/queue/nats/NATSObservableQueue.java`

Simpler pub/sub model:
- `Nats.connect()` → blocking
- Subscribe: `conn.subscribe(subject)` or `conn.subscribe(subject, queue)` for queue groups
- Publish: `conn.publish(subject, data)`
- Monitor thread at 500ms intervals checks connection, reconnects and resubscribes if needed

## NATS Streaming Flow

**File:** `event-queue/nats-streaming/src/main/java/com/netflix/conductor/contribs/queue/stan/NATSStreamObservableQueue.java`

- `StreamingConnectionFactory` with unique `clientId` (UUID)
- Durable subscriptions: `SubscriptionOptions.Builder().durableName(name).build()`
- Queue group support: `conn.subscribe(subject, queue, callback, options)`
- Same monitor pattern as NATS core (500ms polling)

## Caching Pattern (All Providers)

All `EventQueueProvider` implementations cache queues in a `ConcurrentHashMap<String, ObservableQueue>`:

```java
protected Map<String, AMQPObservableQueue> queues = new ConcurrentHashMap<>();

public ObservableQueue getQueue(String queueURI) {
    return queues.computeIfAbsent(queueURI, q -> createQueue(q));
}
```

Queues are created once per URI and reused for the lifetime of the application.

## Default Event Queues

When `conductor.default-event-queue.type` matches a provider (e.g., `amqp`), the configuration creates a `Map<TaskModel.Status, ObservableQueue>` bean for workflow status event routing. These queues use hardcoded names based on task completion/failure status.

## Error Handling Summary

| Module | Connection Failure | Message Processing Failure | Channel Failure |
|---|---|---|---|
| AMQP | Retry loop with `AMQPRetryPattern` | ACK/NACK with retry | Pool recycles channels |
| NATS JetStream | Async reconnect via listener | Manual ACK; message redelivered on failure | N/A (connection-level) |
| NATS Core | Monitor thread reconnects at 500ms | No ACK (at-most-once) | N/A |
| NATS Streaming | Monitor thread reconnects at 500ms | Durable redelivery | N/A |
