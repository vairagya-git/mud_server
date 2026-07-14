# Option Snapshot IV Metrics

This cronjob calculates daily implied-volatility extremes for active option contracts and stores the results in `option_snapshot_iv_metric`.

## Purpose

- Run after market close, controlled by `system_config` purpose `OptionSnapshotIVMetrics`.
- Execute only when enabled, the configured cron expression matches, the current Lisbon time is at or after cutoff, and the job has not already been processed for the current date.

## Source Data

- `option_contract.status = ACTIVE`
- `option_snapshot.option_quote_time`
- `option_snapshot.implied_volatility`

## Metric Rules

- `stock_id` comes from `option_contract.stock_id`
- `option_contract_id` comes from `option_contract.id`
- `iv_date` is derived from `DATE(option_snapshot.option_quote_time)`
- `max_iv` is the maximum `implied_volatility` for the contract on that date
- `max_iv_time` is the earliest `option_quote_time` where `implied_volatility = max_iv`
- `max_iv_stock_distance` is the absolute difference between `option_snapshot.underlying_price` and `option_contract.strike_price` at the `max_iv_time` snapshot
- `min_iv` is the minimum `implied_volatility` for the contract on that date
- `min_iv_time` is the earliest `option_quote_time` where `implied_volatility = min_iv`
- `min_iv_stock_distance` is the absolute difference between `option_snapshot.underlying_price` and `option_contract.strike_price` at the `min_iv_time` snapshot

## Table

`option_snapshot_iv_metric`

- `id`
- `stock_id`
- `option_contract_id`
- `max_iv`
- `max_iv_time`
- `max_iv_stock_distance`
- `min_iv`
- `min_iv_time`
- `min_iv_stock_distance`
- `iv_date`
- `created_at`
- `updated_at`

## Uniqueness

- One row per `stock_id`, `option_contract_id`, and `iv_date`.
- Re-running the job on the same day updates the existing row instead of creating duplicates.