# Cronjob Scheduling Logic

This document explains the current scheduling contract used by all cronjobs through the shared base class `AbstractCronjob`.

## Core Entry Point

Use:

- `shouldExecuteBySchedule(purpose, executionMode)`

This method is the single decision gate before cronjob work executes.

## Decision Order

The gate evaluates in this order:

1. **Force execute**
   - If `forceExecute=true`, execution is allowed immediately.
   - Other schedule checks are bypassed.

2. **Enabled**
   - If `enabled=false`, execution is blocked.

3. **Execution type from config (`execution`)**
   - Values: `daily`, `hourly`, `minutes`.

4. **Type-specific behavior**
   - **daily**:
     - Must **not** have executed already today (`lastUpdated` date check).
     - Must be on/after configured `cutOffTime`.
   - **hourly / minutes**:
     - Additional gating depends on `ExecutionMode` argument:
       - `CUT_OFF`: current time must be on/after `cutOffTime`
       - `BETWEEN_TIME`: current time must be within `[startTime, endTime]`
       - `NONE`: no additional time gate

## Timezone and Formats

- All runtime time checks use Lisbon timezone (`ApplicationConfig.LISBON`).
- Time parsing uses `ApplicationConfig.TIME_FORMAT_HH_MM`.

## Last Updated Rules

- `lastUpdated` is persisted in UTC timestamp format, for example:
  - `2026-07-18T10:27:09.432373Z`
- `lastUpdated` gating is applied only for `daily` execution type.
- For `hourly` and `minutes`, `lastUpdated` is still updated after successful runs, but it does not block execution.

## Recommended Usage in Cronjobs

- Jobs with no extra time window/cutoff for hourly/minutes:
  - call `shouldExecuteBySchedule(purpose, ExecutionMode.NONE)`
- Jobs requiring execution window:
  - call `shouldExecuteBySchedule(purpose, ExecutionMode.BETWEEN_TIME)`
- Jobs requiring cutoff for hourly/minutes:
  - call `shouldExecuteBySchedule(purpose, ExecutionMode.CUT_OFF)`

## Why this design

- Keeps all scheduling behavior centralized.
- Reduces duplicated checks in individual cronjobs.
- Makes daily-vs-hourly/minutes behavior explicit and predictable.
