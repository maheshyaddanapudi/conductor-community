# Safety Rules

## Files That Must Not Be Modified
- `.github/` — CI/CD workflows and issue templates
- `release.sh` — release script
- `gradlew`, `gradlew.bat` — Gradle wrapper executables
- `gradle/` — Gradle wrapper configuration
- `LICENSE`, `licenseheader.txt` — license files
- `dependencies.lock` — dependency lock file

## Files That Must Not Be Read
- `.env*` files
- `**/secrets/**`
- Any files containing credentials or passwords

## Publishing Restrictions
- Never run Gradle tasks that publish artifacts: `snapshot`, `publish`, `final`, `candidate`
- Never push to `main` branch without explicit user approval
- Never modify CI/CD pipeline configuration

## Data Safety
- Never log, print, or commit secrets, API keys, or database credentials
- Do not hardcode connection strings or passwords in source code
- Use Spring Boot `@ConfigurationProperties` for sensitive configuration
