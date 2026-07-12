# Database Schema Notes

## Purpose
Document core tables and relationships relevant to current MudStock features.

## Key Domains
- Analyst ratings
- Day stock movement
- Option analysis / option contracts
- System config
- Market holidays

## Important Tables
- `firm`, `firm_analyst`, `firm_analyst_stock_rating`
- `day_stock_movement_key`, `day_stock_movement_map`, `day_stock_movement_entry`
- `options_interval_analyse`, `option_contract`
- `system_config`
- `master_market_holidays`

## Conventions
- Use explicit SQL in repositories where query complexity is high.
- Keep MySQL compatibility in mind (`DISTINCT` + `ORDER BY` behavior, enum constraints, FK delete order).

## Change Management
- If schema is changed, align:
  - Java entity/repository assumptions
  - `db/Database_Schema.sql` reference
  - any data normalization rules in facades/services
