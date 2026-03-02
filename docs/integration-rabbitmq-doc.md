# Integration: RabbitMQ (AMQP)

## Summary

| Aspect | Detail |
|---|---|
| **Protocol** | AMQP 0-9-1 |
| **Library** | RabbitMQ AMQP Client 5.13.0 |
| **Module** | `event-queue/amqp` |
| **Direction** | Bidirectional (publish + subscribe) |
| **Activation** | `conductor.event-queues.amqp.enabled=true` |

## Architecture

The AMQP integration provides Conductor event queue support via RabbitMQ. It implements two provider types — one for direct queues (`amqp_queue:`) and one for exchanges (`amqp_exchange:`). Both share the same connection infrastructure.

### Key Classes

| Class | File | Responsibility |
|---|---|---|
| `AMQPObservableQueue` | `event-queue/amqp/src/main/java/.../AMQPObservableQueue.java` | Queue/exchange operations (publish, subscribe, ack/nack) |
| `AMQPEventQueueProvider` | `event-queue/amqp/src/main/java/.../config/AMQPEventQueueProvider.java` | Creates/caches ObservableQueue instances |
| `AMQPConnection` | `event-queue/amqp/src/main/java/.../AMQPConnection.java` | Singleton connection + channel pool management |
| `AMQPSettings` | `event-queue/amqp/src/main/java/.../util/AMQPSettings.java` | URI parser for queue/exchange parameters |
| `AMQPEventQueueProperties` | `event-queue/amqp/src/main/java/.../config/AMQPEventQueueProperties.java` | Spring Boot configuration binding |
| `AMQPEventQueueConfiguration` | `event-queue/amqp/src/main/java/.../config/AMQPEventQueueConfiguration.java` | Bean registration |

## Data Flow

### Inbound (Subscribe)

```
RabbitMQ Broker
  └─ AMQPConnection (subscriber connection)
       └─ Channel (reserved per queue)
            └─ DefaultConsumer.handleDelivery()
                 └─ AMQPObservableQueue
                      └─ Observable<Message> → Conductor EventProcessor
```

Two subscription modes (controlled by `sequentialMsgProcessing`):
1. **Sequential** (default): `Observable.interval(100ms)` polls via `channel.basicGet()`
2. **Event-driven**: `DefaultConsumer` pushes messages directly to subscriber threads

### Outbound (Publish)

```
Conductor EventProcessor
  └─ AMQPObservableQueue.publish(messages)
       └─ AMQPConnection.borrowChannel(PUBLISHER)
            └─ channel.basicPublish(exchange, routingKey, props, body)
                 └─ AMQPConnection.returnChannel()
```

## Connection Lifecycle

- **Singleton**: One `AMQPConnection` instance per factory+addresses combination
- **Dual connections**: Separate TCP connections for publishing and subscribing
- **Auto-recovery**: `factory.setAutomaticRecoveryEnabled(true)`, `setTopologyRecoveryEnabled(true)`
- **Network recovery interval**: `networkRecoveryIntervalInMilliSecs` (default 5000ms)
- **Heartbeat**: `requestHeartbeatTimeoutInSecs` (default 30s)
- **SSL**: Optional via `useSslProtocol` property

### Channel Pool

- `ConcurrentHashMap<ConnectionType, Set<Channel>>` — reusable channel pool
- `borrowChannel()` / `returnChannel()` — synchronized access
- Subscriber channels reserved per queue name in `subscriberReservedChannelPool`
- Max channels: `maxChannelCount` (default 5000)

## Retry / Error Handling

### Connection Retry

`AMQPRetryPattern` with configurable `limit` (default 50) and `duration` (default 1000ms):

```java
for (int retryIndex = 0; ...) {
    try {
        connection = factory.newConnection(addresses);
        break;
    } catch (IOException | TimeoutException e) {
        retry.continueOrPropogate(e, retryIndex);
    }
}
```

### Message ACK/NACK Retry

Both `ack()` and `nack()` methods use the same retry pattern:
```java
while (true) {
    try {
        channel.basicAck(deliveryTag, false);
        break;
    } catch (Exception e) {
        retry.continueOrPropogate(e, retryIndex++);
    }
}
```

### Failure Behavior

| Failure | Behavior |
|---|---|
| Connection lost | Auto-recovery reconnects; topology (queues/exchanges) re-declared |
| Channel error | Channel returned to pool; new channel created on next borrow |
| Publish failure | Exception propagated to caller after retry exhaustion |
| Consumer failure | Message not ACK'd; redelivered by RabbitMQ |
| All retries exhausted | `AMQPRetryPattern.continueOrPropogate()` throws final exception |

## Configuration

### Required Properties

```properties
conductor.event-queues.amqp.enabled=true
conductor.event-queues.amqp.hosts=localhost
```

### Key Properties (`AMQPEventQueueProperties`)

| Property | Default | Description |
|---|---|---|
| `hosts` | `localhost` | RabbitMQ server addresses |
| `port` | `5672` | AMQP port |
| `username` | `guest` | Authentication |
| `password` | `guest` | Authentication |
| `virtualHost` | `/` | Virtual host |
| `batchSize` | `1` | `basicQos` prefetch count |
| `pollTimeDuration` | `100ms` | Polling interval (sequential mode) |
| `durable` | `true` | Queue/exchange durability |
| `exclusive` | `false` | Queue exclusivity |
| `autoDelete` | `false` | Auto-delete on last consumer disconnect |
| `exchangeType` | `topic` | Exchange type (topic, direct, fanout) |
| `queueType` | `classic` | Queue type (classic, quorum) |
| `deliveryMode` | `2` | 1=transient, 2=persistent |
| `contentType` | `application/json` | Message content type |
| `sequentialMsgProcessing` | `true` | Sequential vs event-driven mode |
| `useExchange` | `true` | Use exchange-based routing |
| `useSslProtocol` | `false` | Enable SSL/TLS |
| `connectionTimeoutInMilliSecs` | `180000` | Connection timeout |
| `limit` | `50` | Retry attempt limit |
| `duration` | `1000` | Retry interval (ms) |
| `maxChannelCount` | `5000` | Max channels per connection |

### URI Format

```
amqp_queue:myQueueName?durable=true&exclusive=false&autoDelete=false
amqp_exchange:myExchange?exchangeType=topic&routingKey=key&bindQueueName=myQueue
```
