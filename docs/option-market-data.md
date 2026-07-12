# Option Market Data

## Purpose
Capture market-data sources, URL patterns, and parsing rules used for option workflows.

## Data Sources
- Massive snapshot endpoint
- Local mock endpoint (when used for testing)

## Key URL Pattern
- `/v3/snapshot/options/{TICKER}?strike_price={STRIKE}&expiration_date={DATE}&apiKey={API_KEY}`

## Data Flow
1. Read option_to_analyse entries.
2. Build strike ladder from range + interval.
3. Request snapshots per strike and expiration.
4. Persist contract details.

## Error Handling
- Log request/response failures with ticker + strike + expiration context.
- Continue processing remaining entries where possible.

## Operational Notes
- Keep API-key handling in configuration.
- Avoid noisy logs in normal runs; retain sufficient context for debugging.
