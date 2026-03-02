Investigate and fix: $ARGUMENTS

Workflow:

1. **Reproduce**: Identify expected vs actual behavior. If a test failure, run the specific test with `./gradlew :{module}:test --tests "*.{TestClass}" --info`.
2. **Search**: Find the relevant implementation by searching for related classes, configuration properties, and error messages in the codebase.
3. **Root cause**: Trace the code path from entry point (Configuration → Bean → DAO/Provider/Task) to identify the root cause. Check `@ConditionalOnProperty` activation if beans are missing.
4. **Propose fix**: Describe the minimal fix before implementing. Get user approval if the change is non-trivial.
5. **Implement**: Delegate to `implementer` to apply the fix. For persistence issues, involve `persistence-specialist`. For event queue issues, involve `event-queue-specialist`.
6. **Test**: Delegate to `test-engineer` to add or update a regression test covering the fix.
7. **Verify**: Run `./gradlew spotlessApply` then `./gradlew :{module}:test` to confirm the fix.
8. **Summarize**:
   - Root cause identified
   - Fix applied (files changed)
   - Regression test added
   - Verification results
