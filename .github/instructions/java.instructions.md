---
applyTo: "src/main/java/**/*.java"
---

# Java Instructions

- Keep changes small and scoped; avoid broad refactors.
- Preserve package structure and naming conventions in this repository.
- Maintain current controller behavior for HTMX pages:
  - check `HX-Request`
  - return fragment for HX and full template for direct loads
- Follow existing service/facade split:
  - controller orchestrates request flow
  - facade/service contains business logic
  - repository handles persistence details
- Use existing helper utilities where available (`MudDateUtil`, shared parsers, etc.).
- Normalize incoming API values before persistence where domain rules already exist.
- For cronjob-related Java changes:
  - keep `@Profile("cronjob")` behavior intact
  - avoid changing scheduler timing behavior unless explicitly requested
- Add logs where useful for operations/errors, but avoid noisy per-line debug output unless needed.
- Keep methods readable and avoid introducing new dependencies for small tasks.
