# index/es7-persistence

This supplements the root CLAUDE.md. Read that first.

Elasticsearch 7 indexing module. Implements `IndexDAO` for workflow/task search via the ES REST high-level client (7.17.13).

## Build Differences

This module uses the **Shadow JAR plugin** (7.0.0) instead of the standard `jar` task:
- `jar.enabled = false` — the standard jar is disabled
- `shadowJar` replaces it with a fat JAR that shades dependencies
- Service files are merged via `mergeServiceFiles` for `META-INF/services/*` and `META-INF/maven/*`
- The `shadow` configuration extends `compileOnly` so shaded deps are available to tests

The ES version is overridden in build.gradle: `ext['elasticsearch.version'] = revElasticSearch7`

## Key Files

- `ElasticSearchRestDAOV7.java` — primary DAO implementation (highest-churn file in module, 8 commits)
- `ElasticSearchV7Configuration.java` — Spring config using custom `@Conditional(ElasticSearchConditions.ElasticSearchV7Enabled.class)` (not the standard `@ConditionalOnProperty`)
- `ElasticSearchConditions.java` — custom condition class checking both indexing enablement and ES version
- `ElasticSearchBaseDAO.java` — shared base class for index operations
- `dao/query/parser/` — custom query parser (Expression, GroupedExpression, NameValue) with internal AST nodes

## Gotchas

- **Shadow JAR classpath**: If you see `ClassNotFoundException` for ES classes at runtime, check the shadow configuration in `build.gradle`. The `compileOnly` → `shadow` extension is critical.
- **Custom Condition class**: Unlike other modules that use `@ConditionalOnProperty`, this module uses `ElasticSearchConditions.ElasticSearchV7Enabled` which checks multiple properties. New config beans must use this same condition.
- **Tests require Testcontainers Elasticsearch**: The `ElasticSearchTest` base class starts an ES container statically. Docker must be running.
- **Awaitility in tests**: ES tests use `awaitility` for async index refresh assertions — results are not immediately available after indexing.
