Perform a comprehensive code review of current changes.

1. Run `git diff --name-only main...HEAD` to identify all changed files.
2. Delegate to `reviewer` for a structured review against the project checklist (formatting, Spring configuration, persistence patterns, test standards, build files).
3. If any DAO files, SQL migrations, REST controllers, or security-related code changed, also delegate to `security-auditor` for a security audit.
4. If persistence or migration files changed, delegate to `persistence-specialist` to verify Flyway naming, DAO patterns, and cross-database consistency.
5. If event queue files changed, delegate to `event-queue-specialist` to verify provider contracts and connection handling.
6. If new source files lack corresponding tests, delegate to `test-engineer` to assess coverage gaps and recommend tests.
7. Compile a final report with:
   - **Critical**: Issues that will cause build failure or runtime errors
   - **Major**: Convention violations or architectural issues
   - **Minor**: Style improvements and suggestions
   - **Recommended next steps**
