---
paths:
  - '**/es7/**/*.java'
  - '**/index/**/*.java'
---

# Elasticsearch Module Rules

## Conditional Activation

- **NEVER** use `@ConditionalOnProperty` directly for ES7 beans — this module uses a custom `@Conditional(ElasticSearchConditions.ElasticSearchV7Enabled.class)` that checks both `conductor.indexing.enabled=true` AND `conductor.elasticsearch.version=7`. All new beans must use this same condition.
- `ElasticSearchConditions` is an `AllNestedConditions` class — both inner conditions must match.

## Shadow JAR Build

- **NEVER** add `implementation` dependencies that should be shaded — use `compileOnly` instead. The `shadow` configuration extends `compileOnly` so shaded deps are available to both compilation and tests.
- **NEVER** re-enable the `jar` task — it is explicitly disabled (`jar.enabled = false`) and replaced by `shadowJar`.
- **ALWAYS** add service files to the `mergeServiceFiles` block if a new dependency uses `META-INF/services/` for SPI loading.

## DAO Implementation

- **ALWAYS** extend `ElasticSearchBaseDAO` for new index operations — it provides shared REST client access and utility methods.
- `ElasticSearchRestDAOV7` is the primary DAO (highest-churn file in module). Changes here should be carefully reviewed for backward compatibility with existing ES7 indices.

## Query Parser

- The `dao/query/parser/` package implements a custom query language parser with an AST:
  - `Expression`, `GroupedExpression` — top-level query nodes
  - `NameValue` — field comparison nodes
  - `internal/` — operators (`BooleanOp`, `ComparisonOp`), values (`ConstValue`, `ListConst`), and base classes (`AbstractNode`)
- **NEVER** modify the parser grammar without testing against all existing query patterns — the parser is used to translate Conductor query strings into Elasticsearch queries.

## Testing

- **ALWAYS** use `awaitility` for assertions after indexing operations — Elasticsearch indexing is async and results are not immediately available after a write.
- Tests start an Elasticsearch container statically via Testcontainers — Docker must be running.
