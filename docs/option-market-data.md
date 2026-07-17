# Option Market Data

## Purpose
Capture market-data sources, URL patterns, and parsing rules used for option workflows.

## Data Sources
- Massive snapshot endpoint
- Local mock endpoint (when used for testing)

## Key URL Pattern
- `/v3/snapshot/options/{TICKER}?strike_price={STRIKE}&expiration_date={DATE}&apiKey={API_KEY}`

## Data Flow
1. Read options_interval_analyse entries.
2. Build strike ladder from range + interval.
3. Request snapshots per strike and expiration.
4. Persist contract details.

## Error Handling
- Log request/response failures with ticker + strike + expiration context.
- Continue processing remaining entries where possible.

## Operational Notes
- Keep API-key handling in configuration.
- Avoid noisy logs in normal runs; retain sufficient context for debugging.

## Timestamp Policy
- Store option snapshot quote timestamps in UTC in the database.
- Do not change persisted timezone values during writes.
- Convert UTC timestamps to local time only when preparing data for frontend responses.
- Keep backend/domain processing that depends on canonical snapshot time aligned to UTC source values.
