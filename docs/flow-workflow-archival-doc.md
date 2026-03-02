# Flow: Workflow Archival

## Overview

When workflows complete or terminate, the archival listener removes them from the primary execution store. This is the only lifecycle hook implemented in the community modules. Two variants exist: immediate archival and delayed (TTL-based) archival.

## Entry Point

**Interface:** `WorkflowStatusListener` (from `conductor-core`)
**Implementations:**
- `ArchivingWorkflowStatusListener` (`workflow-event-listener/src/main/java/.../archive/ArchivingWorkflowStatusListener.java`)
- `ArchivingWithTTLWorkflowStatusListener` (`workflow-event-listener/src/main/java/.../archive/ArchivingWithTTLWorkflowStatusListener.java`)

## Activation

**Class:** `ArchivingWorkflowListenerConfiguration` (`workflow-event-listener/src/main/java/.../archive/ArchivingWorkflowListenerConfiguration.java`)

```
@ConditionalOnProperty(name = "conductor.workflow-status-listener.type", havingValue = "archive")
```

The configuration chooses which implementation based on `ArchivingWorkflowListenerProperties`:
- If `workflowArchivalDelay > 0` → `ArchivingWithTTLWorkflowStatusListener`
- Otherwise → `ArchivingWorkflowStatusListener`

## Immediate Archival Flow

```
Conductor Core WorkflowExecutor
  └─ onWorkflowCompleted(WorkflowModel) / onWorkflowTerminated(WorkflowModel)
       ├─ Log: "Archiving workflow {id} on completion/termination"
       ├─ executionDAOFacade.removeWorkflow(workflowId, true)
       │    └─ Removes from execution store, archives to index
       └─ Monitors.recordWorkflowArchived(workflowName, status)
```

**Behavior**: Synchronous. The workflow is removed immediately within the same thread that completed/terminated it. The `true` parameter to `removeWorkflow` means "archive" — the workflow data is preserved in the index (Elasticsearch or PostgreSQL index) but removed from the primary execution tables.

## Delayed Archival Flow (TTL)

```
Conductor Core WorkflowExecutor
  └─ onWorkflowCompleted(WorkflowModel) / onWorkflowTerminated(WorkflowModel)
       ├─ Log: "Archiving workflow {id} on completion/termination"
       └─ if delayArchiveSeconds > 0:
            scheduledThreadPoolExecutor.schedule(
                DelayArchiveWorkflow(workflow, executionDAOFacade),
                delayArchiveSeconds, SECONDS)
          else:
            executionDAOFacade.removeWorkflow(workflowId, true)
```

### DelayArchiveWorkflow (Inner Runnable)

```
DelayArchiveWorkflow.run()
  ├─ executionDAOFacade.removeWorkflow(workflowId, true)
  ├─ Log: "Archived workflow {id}"
  ├─ Monitors.recordWorkflowArchived(workflowName, status)
  └─ Monitors.recordArchivalDelayQueueSize(executor.getQueue().size())
```

### Thread Pool Configuration

- **Pool size**: `ArchivingWorkflowListenerProperties.getDelayQueueWorkerThreadCount()`
- **Rejection policy**: Custom handler logs warning + records `Monitors.recordDiscardedArchivalCount()`
- **Cancel policy**: `setRemoveOnCancelPolicy(true)` — removes cancelled tasks from queue immediately
- **Shutdown** (`@PreDestroy`):
  1. `shutdown()` — stop accepting new tasks
  2. `awaitTermination(delayArchiveSeconds)` — wait for in-flight tasks
  3. If timeout: `shutdownNow()` — force stop

## Error Handling

| Scenario | Handling |
|---|---|
| `removeWorkflow` fails (immediate) | Exception propagates to caller (WorkflowExecutor) |
| `removeWorkflow` fails (delayed) | Caught in `DelayArchiveWorkflow.run()`, logged as error, task silently fails |
| Thread pool full (delayed) | `RejectedExecutionHandler` logs warning, records discard metric |
| Shutdown interrupted | `shutdownNow()` called, thread interrupt flag restored |

## Metrics Emitted

| Metric | Source | Description |
|---|---|---|
| `workflow_archived` | `Monitors.recordWorkflowArchived(name, status)` | Counter per workflow name and terminal status |
| `discarded_archival_count` | `Monitors.recordDiscardedArchivalCount()` | Counter for rejected archival tasks |
| `archival_delay_queue_size` | `Monitors.recordArchivalDelayQueueSize(size)` | Gauge of pending delayed archival tasks |

## Configuration Properties

**Class:** `ArchivingWorkflowListenerProperties`

| Property | Description | Effect |
|---|---|---|
| `ttlDuration` | TTL duration (note: TTL removal is no longer supported — logged as warning) | Used for `archiveTTLSeconds` but not applied |
| `workflowArchivalDelay` | Seconds to delay archival after completion/termination | `> 0` enables delayed mode |
| `delayQueueWorkerThreadCount` | Thread pool size for delayed archival | Controls parallelism |

## Cross-Module Interactions

- **conductor-core**: `ExecutionDAOFacade.removeWorkflow()` delegates to the active `ExecutionDAO` and `IndexDAO`
- **Persistence modules**: The removal cascades through whichever DAO is active (MySQL, PostgreSQL, or in-memory)
- **Index modules**: If indexing is enabled, the workflow is preserved in the index during archival

## Alternative: Queue Publisher

A second listener type exists (`conductor.workflow-status-listener.type=queue_publisher`):

**Class:** `ConductorQueueStatusPublisher` (`workflow-event-listener/src/main/java/.../conductorqueue/ConductorQueueStatusPublisherConfiguration.java`)

This publishes workflow status changes to Conductor's internal queue system instead of archiving. It implements the same `WorkflowStatusListener` interface.
