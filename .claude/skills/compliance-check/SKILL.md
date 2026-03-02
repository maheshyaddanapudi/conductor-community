---
name: compliance-check
description: |
  Check code for compliance with project standards. Use when user says:
  "check compliance", "verify standards", "check formatting", "run linter",
  "spotless check", "compliance check", "code standards", "pre-commit check"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Compliance Check

Verify code changes comply with all project standards before commit.

## Workflow

1. **Identify changed files**:
   ```bash
   git diff --name-only HEAD
   git diff --name-only --cached
   ```

2. **Run Spotless formatting check**:
   ```bash
   ./gradlew spotlessCheck
   ```
   If failures found, report them and offer to auto-fix with `./gradlew spotlessApply`.

3. **Check license headers** on new Java files:
   - Every `.java` file must start with the Apache 2.0 header from `licenseheader.txt`
   - Spotless enforces this, but verify explicitly for new files

4. **Check import ordering** on changed Java files:
   - Required order: `java` → `javax` → `org` → `com.netflix` → others → static `com.netflix` → static others
   - Spotless enforces this via `importOrder()` configuration

5. **Check for manual lock file edits**:
   ```bash
   git diff --name-only | grep "dependencies.lock"
   ```
   If `dependencies.lock` files were manually edited, warn that they should only be modified via `./gradlew generateLock saveLock`.

6. **Check `@ConditionalOnProperty` consistency** on new Configuration classes:
   - Verify each new `@Configuration` class has appropriate conditional gating
   - Cross-reference with the activation property convention in `.claude/rules/spring-configuration.md`

7. **Check Flyway migration naming** if SQL files were added:
   - Verify `V{N}__` naming with correct next version number
   - Verify no existing migration was modified

8. **Report** all findings with pass/fail status for each check.
