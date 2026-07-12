# Copilot Instructions for MudStock

## Project Stack and Scope
- Backend: Spring Boot (Java), mixed data access using Spring Data JPA and JdbcTemplate repositories.
- Frontend: Thymeleaf templates with HTMX partial rendering and shared styles in `src/main/resources/static/css/app.css`.
- Database: MySQL.
- Profiles: `server`, `local`, `cronjob`.

## Core Development Rules
- Keep changes minimal and scoped to the requested feature/fix.
- Do not refactor unrelated modules in the same change.
- Preserve existing URL paths and template names unless explicitly requested.
- Prefer consistency with existing naming and package structure.

## Controller + HTMX Pattern
- List/detail pages should support both full-page and HTMX fragment rendering.
- Follow current convention:
  - Read `HX-Request` header.
  - Return `template :: content` for HTMX.
  - Return full `template` otherwise.
- Use `#page-content` as the HTMX swap target in templates.

## UI and Template Guidelines
- Reuse existing CSS utility classes and button patterns.
- Add new shared styling to `app.css` instead of duplicating styles in templates when possible.
- For table screens, keep filtering/sorting browser-side unless server-side filtering is explicitly requested.
- Prefer adding controls that match existing `filter-bar`, chip, and sort-button behavior.

## Data Access Guidelines
- For complex query screens, follow existing JdbcTemplate repository style.
- For entity CRUD, prefer existing Spring Data repositories where already used.
- Keep SQL explicit and compatible with current MySQL behavior.
- Be careful with `DISTINCT` + `ORDER BY` compatibility in MySQL.

## Domain-Specific Rules (Current Behavior)
- Analyst ratings ingestion:
  - Normalize missing `previous_rating` to `none` before persistence.
  - Normalize missing `price_target_action` to `none` before persistence.
- Day stock movement entries screen:
  - Existing filter/sort interactions are browser-based and should remain browser-based unless requested otherwise.

## Cronjob and Logging Conventions
- Cronjobs are profile-gated with `@Profile("cronjob")`.
- Cronjob logs use dedicated rolling file appenders in `logback-spring.xml`.
- When adding/changing cronjobs, update both:
  - `src/main/resources/logback-spring.xml`
  - `src/main/resources/application-cronjob.yml` log file mapping.

## File and Config Placement
- Templates: `src/main/resources/templates/...`
- Static CSS: `src/main/resources/static/css/app.css`
- Main config: `src/main/resources/application.yml`
- Cronjob profile config: `src/main/resources/application-cronjob.yml`
- Logback config: `src/main/resources/logback-spring.xml`

## Validation Before Finishing
- Run compile check after code changes:
  - `mvn -q -DskipTests compile`
- If behavior changed in cron/logging, verify profile-specific config entries are present.

## Avoid
- Introducing new frameworks/libraries for simple tasks.
- Breaking HTMX fragment behavior by returning only full templates.
- Changing database schema in code-only tasks unless explicitly requested.

## Detailed domain documentation

When working on option strategies, market snapshots, strategy legs,
P&L calculations, or rolling logic, read:

- [Option Strategy Model](../docs/option-strategy-model.md)