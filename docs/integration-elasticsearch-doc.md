# Integration: Elasticsearch 7

## Summary

| Aspect | Detail |
|---|---|
| **Protocol** | HTTP REST |
| **Library** | ES REST High-Level Client 7.17.13 |
| **Module** | `index/es7-persistence` |
| **Direction** | Bidirectional (index + search) |
| **Activation** | Custom `@Conditional(ElasticSearchConditions.ElasticSearchV7Enabled.class)` |

## Architecture

The Elasticsearch integration provides search/index capabilities for workflows and tasks. It uses the REST high-level client (not transport) and implements Conductor's `IndexDAO` interface. The module uses the Shadow JAR plugin to shade dependencies, avoiding classpath conflicts.

### Key Classes

| Class | File | Responsibility |
|---|---|---|
| `ElasticSearchRestDAOV7` | `index/es7-persistence/src/main/java/.../es7/dao/index/ElasticSearchRestDAOV7.java` | All index/search operations |
| `ElasticSearchBaseDAO` | `index/es7-persistence/src/main/java/.../es7/dao/index/ElasticSearchBaseDAO.java` | Shared utilities |
| `ElasticSearchV7Configuration` | `index/es7-persistence/src/main/java/.../es7/config/ElasticSearchV7Configuration.java` | Bean registration |
| `ElasticSearchConditions` | `index/es7-persistence/src/main/java/.../es7/config/ElasticSearchConditions.java` | Custom activation condition |
| Query parser classes | `index/es7-persistence/src/main/java/.../es7/dao/query/parser/` | Conductor query → ES query translation |

## Data Flow

### Indexing (Inbound to ES)

```
Conductor Core
  └─ IndexDAO.indexWorkflow(WorkflowSummary) / indexTask(TaskSummary)
       └─ ElasticSearchRestDAOV7
            ├─ Serialize to JSON
            ├─ Add to BulkRequests buffer (ConcurrentHashMap)
            └─ Async flush when batch size or timeout reached
                 └─ BulkRequest → ES REST client → Elasticsearch cluster
```

### Search (Outbound from ES)

```
Conductor REST API (search endpoints)
  └─ IndexDAO.searchWorkflows(query) / searchTasks(query)
       └─ ElasticSearchRestDAOV7
            ├─ Parse Conductor query syntax via query parser
            ├─ Build SearchRequest with BoolQueryBuilder
            └─ elasticSearchClient.search(request)
                 └─ Parse SearchResponse → List<WorkflowSummary>
```

## Connection Lifecycle

### Client Creation

```java
// ElasticSearchV7Configuration
@Bean
RestClient restClient(RestClientBuilder builder) {
    return builder.build();
}

@Bean
RestClientBuilder restClientBuilder(ElasticSearchProperties properties) {
    // Parses properties.getUrl() → HttpHost[]
    // Sets connection timeouts, max retry timeout
    return RestClient.builder(httpHosts);
}
```

- **Admin client**: Separate `RestClient` for cluster health and index management
- **Search client**: `RestHighLevelClient` for document operations
- **No connection pooling config**: Uses Apache HttpAsyncClient defaults

### Bulk Indexing Buffer

```java
ConcurrentHashMap<String, BulkRequests> bulkRequests
```

- **Thread pool**: `CORE_POOL_SIZE=6`, `KEEP_ALIVE_TIME=1` second
- **Separate executor for logs**: `logExecutorService`
- **Flush triggers**: Configurable batch size (`indexBatchSize`) and timeout (`asyncBufferFlushTimeout`)

## Retry / Error Handling

### Retry Template

```java
// ElasticSearchV7Configuration
@Bean
RetryTemplate es7RetryTemplate() {
    // Spring RetryTemplate with configurable policy
}
```

Operations wrapped in `retryTemplate.execute()` for transient ES failures.

### Index Creation

On startup, `ElasticSearchRestDAOV7` checks for index existence and creates indexes with mappings if missing. Index names follow the pattern `{indexPrefix}_{type}`:
- `{prefix}_workflow`
- `{prefix}_task`
- `{prefix}_task_log`
- `{prefix}_event`
- `{prefix}_message`

### Failure Behavior

| Failure | Behavior |
|---|---|
| ES cluster unreachable | Retry via RetryTemplate; eventually throws |
| Bulk indexing partial failure | Individual failures logged; successful items committed |
| Search query error | Exception propagated to REST API layer |
| Index creation failure | Logged; subsequent operations may fail |

## Configuration

### Required Properties

```properties
conductor.elasticsearch.url=localhost:9200
conductor.elasticsearch.version=7
conductor.indexing.enabled=true
```

### Key Properties (`ElasticSearchProperties`)

| Property | Default | Description |
|---|---|---|
| `url` | `localhost:9300` | ES cluster URL(s) |
| `indexName` / `indexPrefix` | `conductor` | Index name prefix |
| `version` | `6` | ES version (must be 7 for this module) |
| `indexBatchSize` | varies | Bulk request batch size |
| `asyncBufferFlushTimeout` | varies | Bulk flush timeout |

### Custom Activation Condition

Unlike other modules that use `@ConditionalOnProperty`, ES7 uses a custom `Condition`:

```java
@Conditional(ElasticSearchConditions.ElasticSearchV7Enabled.class)
```

This checks both that indexing is enabled and the version is 7.

## Query Parser

The module includes a custom query parser that translates Conductor's query syntax into Elasticsearch queries:

**Package:** `com.netflix.conductor.es7.dao.query.parser`

Key classes:
- `Expression` — top-level query expression
- `GroupedExpression` — parenthesized sub-expressions
- `NameValue` — field=value comparisons
- `ComparisonOp` — comparison operators (=, !=, >, <, IN)
- `ConstValue` — literal values (string, number, range)

The parser builds an AST from the query string, then converts it to Elasticsearch `BoolQueryBuilder` components.

## Build Note: Shadow JAR

The `es7-persistence` module uses the Shadow plugin (7.0.0) to shade Elasticsearch client dependencies. The standard `jar` task is replaced by `shadowJar`. This prevents classpath conflicts when the ES client's transitive dependencies (Lucene, Netty) clash with other modules.
