Create a complete implementation for: $ARGUMENTS

Follow this workflow:

1. Delegate to `planner` to analyze the request, identify affected modules, and produce a step-by-step implementation plan.
2. Review the plan, then delegate to `implementer` to write production code. If the task involves event queues (AMQP, NATS, Kafka), also delegate to `event-queue-specialist`. If it involves persistence or migrations, also delegate to `persistence-specialist`.
3. Delegate to `test-engineer` to create or update tests for the new code.
4. Delegate to `reviewer` for a structured quality review against project conventions.
5. If security-sensitive code changed (credentials, SQL, REST endpoints, deserialization), delegate to `security-auditor`.
6. Run `./gradlew spotlessApply` and `./gradlew build -x test` to verify.
7. Summarize:
   - What was implemented (modules, classes, files)
   - Tests added or updated
   - Review findings and how they were addressed
   - Any follow-up work remaining
