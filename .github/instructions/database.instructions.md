---
applyTo: "src/main/java/com/rama/mudstock/repository/**/*.java,db/**/*.sql"
---

# Database Instructions

- Prefer minimal SQL changes targeted to the requested behavior.
- Follow existing JdbcTemplate style for query-heavy screens and Spring Data JPA where already established.
- Keep SQL explicit and MySQL-compatible.
- Be careful with `DISTINCT` + `ORDER BY` in MySQL:
  - Order by selected expressions/aliases when using distinct projections.
- Avoid schema changes in code-only tasks unless explicitly requested.
- If schema edits are requested, update both:
  - runtime assumptions in Java model/repository code
  - `db/Database_Schema.sql` as reference
- For enum-like fields persisted from APIs, normalize missing/blank values before persistence if domain rules require defaults.
- Maintain FK-safe delete order when deleting parent-child data.
- Keep repository logging concise and useful for troubleshooting SQL failures.
