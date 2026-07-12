---
applyTo: "src/main/java/com/rama/mudstock/{controller,facade,repository,service}/**/*Option*.java,src/main/resources/templates/option_analysis/**/*.html"
---

# Options Domain Instructions

- Preserve existing option-analysis routes under `/option-analysis/*`.
- Keep the current HTMX + Thymeleaf pattern:
  - Return `template :: content` for HX requests.
  - Return full template otherwise.
- Prefer focused changes for option screens (`analyse`, `contract`) without refactoring unrelated domains.
- Keep list filtering/sorting browser-side unless explicitly requested as server-side.
- Reuse existing table/filter UI patterns (`filter-bar`, chips, sort buttons).
- For option data writes:
  - Validate required fields early (stock, contract type, expiration, range, interval).
  - Normalize enum-like strings to uppercase where the codebase already does so.
- For external snapshot calls, preserve existing URL-building conventions and logging style.
- Do not change URL signatures or existing template names unless explicitly requested.
