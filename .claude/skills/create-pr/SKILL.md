---
name: create-pr
description: |
  Create a pull request using the project's PR template. Use when user says:
  "create PR", "create pull request", "open PR", "submit PR", "make a PR",
  "push and create PR", "send PR", "open a pull request"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Create Pull Request

Create a pull request following the project's established PR template and conventions.

## Workflow

1. **Pre-flight checks**:
   ```bash
   ./gradlew spotlessCheck
   ./gradlew build -x test
   ```
   If either fails, fix issues before proceeding.

2. **Analyze changes** to classify the PR type:
   ```bash
   git diff main --stat
   git log main..HEAD --oneline
   ```
   Classify as one of: Bugfix, Feature, Refactoring, Build related changes, Other.

3. **Check if dependency locks need refresh** (for build-related changes):
   ```bash
   git diff main --name-only | grep -E "dependencies\.gradle|build\.gradle"
   ```
   If build files changed, remind user to run `./gradlew generateLock saveLock`.

4. **Generate PR body** matching `.github/pull_request_template.md`:
   ```markdown
   Pull Request type
   ----

   - [x] {type}  (check the appropriate one)

   Changes in this PR
   ----

   {Description of changes and why they're needed}
   Issue #{number if applicable}

   Alternatives considered
   ----

   {Alternative approaches that were considered, or "N/A" for straightforward changes}
   ```

5. **Ensure branch is pushed**:
   ```bash
   git push -u origin {branch-name}
   ```

6. **Create the PR** against `main`:
   ```bash
   gh pr create --base main --title "{concise title}" --body "{generated body}"
   ```
   If `gh` is not installed, provide the URL to create the PR manually:
   ```
   https://github.com/{owner}/{repo}/compare/main...{branch}
   ```

7. **Post-creation checklist** — remind the user:
   - CI will run `./gradlew build --scan -x test` (tests are skipped on PRs)
   - Review the PR description for completeness
   - Link any related GitHub Issues
