---
mode: ask
---

Implement an options feature in this repository using existing project patterns.

Requirements:
1. Keep routes under `/option-analysis/*` unless the task explicitly says otherwise.
2. Follow HTMX + Thymeleaf fragment/full-page response behavior.
3. Keep table filtering/sorting browser-side by default.
4. Reuse existing UI styles and components from `app.css` and current option templates.
5. Make minimal, focused Java changes across controller/facade/repository as needed.
6. Ensure persistence changes are MySQL-safe and consistent with existing repository style.
7. Add useful logging for failures and key flow checkpoints.
8. Run `mvn -q -DskipTests compile` after code edits.

Output format:
- Summary of changes
- Files touched
- Validation result
- Any follow-up actions needed
