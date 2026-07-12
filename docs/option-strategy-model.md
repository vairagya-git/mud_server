# Option Strategy Domain Model

## 1. Purpose

This application collects live options market data and tracks the performance of real and simulated multi-leg option strategies.

The application supports strategies such as:

- Long Call
- Long Put
- Long Straddle
- Long Strangle
- Bull Call Spread
- Bear Call Spread
- Bull Put Spread
- Bear Put Spread
- Iron Condor
- Iron Butterfly
- Reverse Iron Condor
- Custom multi-leg strategies

The core design principle is:

> An option strategy is composed of one or more option strategy legs. Each leg references a real option contract. Market data for each contract is recorded periodically as option snapshots.

The application normally collects market data every 5 minutes while the market is open.

---

## 2. Core Domain Model

The main entities are:

1. `stock`
2. `option_contract`
3. `option_snapshot`
4. `option_strategy`
5. `option_strategy_leg`
6. `option_strategy_leg_snapshot`
7. `option_strategy_snapshot`

High-level relationship:

    stock
      |
      +-- option_contract
      |       |
      |       +-- option_snapshot
      |
      +-- option_strategy
              |
              +-- option_strategy_leg
              |       |
              |       +-- option_strategy_leg_snapshot
              |               |
              |               +-- references option_snapshot
              |
              +-- option_strategy_snapshot

---

## 3. Option Contract

### Purpose

`option_contract` represents one specific exchange-traded option instrument.

Examples:

    O:MU260724C00900000
    O:MU260724P00900000

The first example represents a Call option and the second represents a Put option.

An option contract contains information such as:

- Underlying stock
- Contract type: CALL or PUT
- Strike price
- Expiration date
- Exercise style
- Shares per contract
- External contract ticker

### Important Rule

An option contract is either a CALL or PUT.

It is not inherently LONG or SHORT.

LONG or SHORT describes the position taken in that contract and therefore belongs to `option_strategy_leg`.

The same option contract can be:

- LONG in one strategy
- SHORT in another strategy
- Used by multiple different strategies

### Example

    Contract:
        MU 24-Jul-2026 900 Call

    Contract ticker:
        O:MU260724C00900000

    Contract type:
        CALL

    Strike:
        900

    Expiration:
        2026-07-24

---

## 4. Option Snapshot

### Purpose

`option_snapshot` represents the market state of one option contract at a specific point in time.

Market snapshots are normally collected every 5 minutes while the market is open.

Example:

    10:00 -> Option midpoint: 129.82
    10:05 -> Option midpoint: 131.50
    10:10 -> Option midpoint: 135.20

Each snapshot references exactly one `option_contract`.

### Data Stored

An option snapshot may contain:

- Snapshot time
- Option quote time
- Option trade time
- Underlying asset time
- Underlying stock price
- Break-even price
- Change to break-even
- Bid
- Ask
- Midpoint
- Last trade price
- Bid size
- Ask size
- Last trade size
- Implied volatility
- Delta
- Gamma
- Theta
- Vega
- Open interest
- Daily volume
- Quote timeframe
- Underlying timeframe

### Important Rule

`option_snapshot` represents market data only.

It does not know whether the application currently owns the contract.

It does not contain:

- LONG or SHORT position
- Quantity owned
- Entry price
- Exit price
- Position P&L

Those values belong to the strategy domain.

---

## 5. Option Strategy

### Purpose

`option_strategy` represents one complete options position being monitored by the application.

The strategy may represent:

- A real live trade
- A simulated trade using real market data

### Strategy Mode

Supported modes:

    LIVE_TRADE
    SIMULATION

`LIVE_TRADE` represents a real position opened with a broker such as IBKR.

`SIMULATION` represents a virtual position created by the application and monitored against actual live market data.

Both modes use the same market snapshots.

### Strategy Types

Examples:

    LONG_CALL
    LONG_PUT
    LONG_STRADDLE
    LONG_STRANGLE
    BULL_CALL_SPREAD
    BEAR_CALL_SPREAD
    BULL_PUT_SPREAD
    BEAR_PUT_SPREAD
    IRON_CONDOR
    IRON_BUTTERFLY
    REVERSE_IRON_CONDOR
    CUSTOM

The strategy type describes the logical structure of the position.

The actual contracts making up the strategy are defined by `option_strategy_leg`.

### Strategy Status

Examples:

    OPEN
    CLOSED
    CANCELLED

Only OPEN strategies are actively monitored and updated every 5 minutes.

---

## 6. Option Strategy Leg

### Purpose

`option_strategy_leg` defines one individual position inside an option strategy.

Every strategy contains one or more legs.

Each leg references exactly one `option_contract`.

The strategy leg defines:

- Which option contract is used
- Whether the position is LONG or SHORT
- Quantity of contracts
- Entry price
- Entry snapshot
- Exit price
- Exit snapshot
- Position status
- Realized P&L

### Position Side

Supported values:

    LONG
    SHORT

### Example: Long Strangle

Strategy:

    LONG_STRANGLE

Leg 1:

    LONG 1000 CALL x 5 contracts

Leg 2:

    LONG 900 PUT x 5 contracts

### Example: Long Straddle

Strategy:

    LONG_STRADDLE

Leg 1:

    LONG 950 CALL x 4 contracts

Leg 2:

    LONG 950 PUT x 4 contracts

### Example: Bull Call Spread

Strategy:

    BULL_CALL_SPREAD

Leg 1:

    LONG 950 CALL x 5 contracts

Leg 2:

    SHORT 1000 CALL x 5 contracts

### Example: Iron Condor

Strategy:

    IRON_CONDOR

Leg 1:

    LONG lower-strike PUT

Leg 2:

    SHORT higher-strike PUT

Leg 3:

    SHORT lower-strike CALL

Leg 4:

    LONG higher-strike CALL

### Entry Snapshot

Every strategy leg should reference the exact `option_snapshot` that was available when the position was entered.

This allows the application to retrieve the original:

- Option price
- Stock price
- Delta
- Gamma
- Theta
- Vega
- Implied volatility
- Bid and ask spread
- Open interest
- Volume

The strategy leg should not duplicate these market values if they are already available through `entry_snapshot_id`.

However, the actual execution price should be stored separately because the execution price may differ from:

- Bid
- Ask
- Midpoint
- Last trade price

---

## 7. Option Strategy Leg Snapshot

### Purpose

`option_strategy_leg_snapshot` tracks the progress of each individual strategy leg every 5 minutes.

This is different from `option_snapshot`.

`option_snapshot` answers:

> What was the market state of this option contract at this time?

`option_strategy_leg_snapshot` answers:

> How was my specific position in this contract performing at this time?

Each strategy leg snapshot references:

- One `option_strategy_leg`
- One exact `option_snapshot`

### Example

A strategy contains:

    Long 2100 Call x 4
    Long 1950 Put x 4

Progress:

    10:00
        Call P&L:      $0
        Put P&L:       $0
        Total P&L:     $0

    10:05
        Call P&L:      +$2,000
        Put P&L:       -$1,600
        Total P&L:     +$400

    10:10
        Call P&L:      +$6,000
        Put P&L:       -$4,000
        Total P&L:     +$2,000

    10:15
        Call P&L:      +$12,000
        Put P&L:       -$7,200
        Total P&L:     +$4,800

### Why This Table Is Important

This allows the application to answer questions such as:

- At exactly what time did the strategy become profitable?
- Which leg caused the strategy to lose money?
- When did the losing leg start creating significant drag?
- How much did the winning leg gain?
- How much did the losing leg lose?
- Did IV collapse before the position turned negative?
- Did Theta increase?
- Did the stock move without sufficient option-price movement?
- Was a wide bid/ask spread affecting the calculated value?

Because the leg snapshot references the exact `option_snapshot`, the application can inspect the exact market conditions at that moment.

---

## 8. Option Strategy Snapshot

### Purpose

`option_strategy_snapshot` represents the total performance of the entire strategy at a specific point in time.

It is calculated every 5 minutes for every OPEN strategy.

### Example

    Time     Stock     Position Value     P&L       P&L %
    ------------------------------------------------------
    10:00    950       $100,000           $0         0.0%
    10:05    958       $102,500           +$2,500    2.5%
    10:10    970       $108,000           +$8,000    8.0%

### Data Stored

The strategy snapshot may contain:

- Strategy ID
- Snapshot time
- Underlying stock price
- Total entry cost
- Current market value
- Unrealized P&L
- Unrealized P&L percentage
- Realized P&L
- Total P&L
- Net Delta
- Net Gamma
- Net Theta
- Net Vega
- Average or weighted IV

### Difference Between Strategy Snapshot and Leg Snapshot

`option_strategy_snapshot` answers:

> How is my complete strategy performing?

`option_strategy_leg_snapshot` answers:

> How is each individual leg contributing to the total strategy result?

Both are valuable.

---

## 9. P&L Calculation

### Long Position

For a LONG option leg:

    Unrealized P&L =
        (Current Option Price - Entry Execution Price)
        x Quantity
        x Shares Per Contract

Example:

    Entry Price:          100
    Current Price:        115
    Quantity:             4
    Shares Per Contract:  100

    P&L =
        (115 - 100) x 4 x 100
        = $6,000

### Short Position

For a SHORT option leg:

    Unrealized P&L =
        (Entry Execution Price - Current Option Price)
        x Quantity
        x Shares Per Contract

Example:

    Entry Price:          50
    Current Price:        40
    Quantity:             4
    Shares Per Contract:  100

    P&L =
        (50 - 40) x 4 x 100
        = $4,000

---

## 10. Option Valuation

Different prices may be used for different purposes.

### Midpoint

    (Bid + Ask) / 2

Preferred for:

- General analysis
- Historical comparison
- Strategy simulation
- P&L trend charts

### Executable Liquidation Value

For a LONG option:

    Use BID

Because a long position would normally be sold to close.

For a SHORT option:

    Use ASK

Because a short position would normally need to be bought back to close.

This gives a more conservative estimate of actual realizable P&L.

### Recommended Approach

The application should support at least two valuations:

    MARK_TO_MID
    EXECUTABLE_EXIT

`MARK_TO_MID` is useful for analysing strategy performance.

`EXECUTABLE_EXIT` is useful for estimating what could actually be realized if the position were closed immediately.

---

## 11. Combined Greeks

Strategy Greeks should account for:

- LONG or SHORT position
- Quantity
- Contract multiplier where appropriate

For Delta:

    Net Delta =
        Sum of signed Delta x quantity

Position sign:

    LONG  = +1
    SHORT = -1

Example:

    Long Call Delta:  +0.60
    Long Put Delta:   -0.40

    Net Delta:
        +0.60 + (-0.40)
        = +0.20

For a short call with Delta +0.60:

    Position Delta:
        -1 x 0.60
        = -0.60

The same position-side principle applies to:

- Gamma
- Theta
- Vega

When displaying dollar exposure, the contract multiplier and quantity should also be included.

---

## 12. Implied Volatility Tracking

IV is one of the most important metrics for this application.

The application should track:

- Current IV
- Entry IV
- IV change since entry
- IV change during the last 5 minutes
- IV change during the last 15 minutes
- IV change during the last 30 minutes
- IV change during the last hour
- Historical IV by contract
- Call IV versus Put IV

This is particularly important for long straddles and long strangles because both legs may lose value even when the underlying stock moves if IV falls significantly.

Example:

    10:00
        Stock: 950
        Call IV: 105%
        Put IV: 104%

    10:30
        Stock: 965
        Call IV: 94%
        Put IV: 93%

The stock moved, but IV collapsed.

The strategy may still lose money.

---

## 13. Moneyness

Moneyness should be calculated using:

    Underlying Price
    Strike Price

A general percentage calculation is:

    Moneyness % =
        ((Underlying Price - Strike Price) / Underlying Price) x 100

However, the semantic interpretation differs between CALL and PUT contracts.

The application should be able to classify options as:

    ITM
    ATM
    OTM

It should also support grouping contracts by approximate moneyness bands such as:

    1%
    2%
    3%
    4%
    5%
    10%
    15%
    20%

This is useful for comparing historical:

- IV
- Delta
- Gamma
- Theta
- Vega
- Liquidity

for similarly positioned options.

---

## 14. Rolling Strategy

A roll should not modify historical strategy data.

When a strategy is rolled:

1. The existing strategy is closed.
2. A new strategy is created.
3. The new strategy references the previous strategy.
4. Both strategies can belong to the same root strategy lifecycle.

Example:

    Strategy 100
        Original position
        Status: CLOSED

            |
            | ROLL
            v

    Strategy 101
        previous_strategy_id = 100
        root_strategy_id = 100

            |
            | ROLL
            v

    Strategy 102
        previous_strategy_id = 101
        root_strategy_id = 100

This allows the application to calculate:

- P&L for each individual strategy
- Realized P&L from each roll
- Current unrealized P&L
- Total lifecycle P&L across all rolls

### Important Rule

Do not mutate an old strategy's historical legs into new contracts after a roll.

Historical trades should remain immutable.

---

## 15. Live Trade vs Simulation

Both LIVE_TRADE and SIMULATION use actual market snapshots.

### LIVE_TRADE

Represents an actual broker position.

The application may compare calculated values with values displayed by IBKR.

Actual execution prices should be stored.

### SIMULATION

Represents a virtual position.

No broker trade is required.

The application selects actual option contracts and tracks their performance against real market data every 5 minutes.

This allows direct comparison between:

- Real trades
- Simulated trades
- Different strategy types
- Different entry times
- Different Delta selections
- Different Theta profiles

---

## 16. Five-Minute Processing Flow

Every 5 minutes while the market is open:

1. Fetch live option data for monitored contracts.
2. Store one `option_snapshot` for each contract.
3. Find all OPEN strategies.
4. Find all OPEN legs for each strategy.
5. Find the latest `option_snapshot` for each leg's contract.
6. Calculate the current value and P&L of each leg.
7. Create one `option_strategy_leg_snapshot` per active leg.
8. Aggregate all leg results.
9. Calculate strategy-level:
   - Current market value
   - Unrealized P&L
   - P&L percentage
   - Net Delta
   - Net Gamma
   - Net Theta
   - Net Vega
   - Average or weighted IV
10. Create one `option_strategy_snapshot`.

---

## 17. Example: Complete Long Strangle Flow

Stock:

    MU = 950

Strategy:

    LONG_STRANGLE
    Mode: SIMULATION

Leg 1:

    LONG 1000 Call x 5
    Entry option price: 80

Leg 2:

    LONG 900 Put x 5
    Entry option price: 60

Initial total cost:

    Call:
        80 x 5 x 100 = $40,000

    Put:
        60 x 5 x 100 = $30,000

    Total:
        $70,000

After 5 minutes:

    Call price: 85
    Put price: 57

Call P&L:

    (85 - 80) x 5 x 100
    = +$2,500

Put P&L:

    (57 - 60) x 5 x 100
    = -$1,500

Total strategy P&L:

    +$2,500 - $1,500
    = +$1,000

The application stores:

- Two new `option_snapshot` records
- Two new `option_strategy_leg_snapshot` records
- One new `option_strategy_snapshot` record

---

## 18. Data Integrity Rules

The following rules should be maintained:

1. An option contract ticker must uniquely identify a contract.
2. LONG or SHORT must never be stored on `option_contract`.
3. Each strategy must contain at least one leg.
4. Each strategy leg must reference exactly one option contract.
5. Every entry snapshot must belong to the same option contract as the strategy leg.
6. Every exit snapshot must belong to the same option contract as the strategy leg.
7. Every strategy leg snapshot must reference an option snapshot for the same option contract.
8. Historical strategy snapshots should not be modified after creation.
9. Historical market snapshots should not be modified after creation.
10. A closed strategy must not receive new progress snapshots.
11. All financial calculations must use decimal arithmetic.
12. Java code must use `BigDecimal` for prices, IV, Greeks, percentages, and P&L.
13. Database columns must use `DECIMAL`, not floating-point types, for financial values.
14. Timestamps should be stored consistently, preferably in UTC.

---

## 19. Source of Truth

The source of truth for each type of data is:

    Contract definition
        -> option_contract

    Historical market state
        -> option_snapshot

    Strategy definition
        -> option_strategy

    Contracts and positions within a strategy
        -> option_strategy_leg

    Individual leg performance over time
        -> option_strategy_leg_snapshot

    Complete strategy performance over time
        -> option_strategy_snapshot

    Actual database structure
        -> SQL migration files

This document describes the domain intent and architectural rules.

The actual SQL migration files remain the source of truth for the physical database schema.

---

## 20. Future Capabilities

The architecture should remain extensible for future capabilities such as:

- Automatic strategy creation based on Delta
- Automatic strike selection
- Automatic straddle and strangle generation
- Entry filters based on Theta
- Overnight volatility strategy screening
- Gamma-to-Theta efficiency ranking
- Vega-to-Theta analysis
- IV expansion and IV crush detection
- Automatic profit harvesting
- Stop-loss and trailing-stop simulation
- Roll recommendations
- Strategy lifecycle P&L across multiple rolls
- Comparison of simulated positions against actual IBKR trades
- Historical strategy replay
- Strategy performance analytics by stock
- Strategy performance analytics by entry Delta
- Strategy performance analytics by Theta
- Strategy performance analytics by moneyness
- Detection of the exact 5-minute period where a strategy started losing money

The initial implementation should remain simple and should not add unnecessary tables until a concrete use case requires them.

---

## 21. Option Snapshot Field Reference

### Purpose

`option_snapshot` is the central historical market-data table for the options application.

It records the state of one option contract at one point in time.

The table is used for:

- Tracking IV changes.
- Tracking Greek changes.
- Calculating strategy-leg value.
- Calculating complete strategy P&L.
- Detecting liquidity changes.
- Detecting unusual option volume.
- Identifying the exact time a position started gaining or losing value.
- Replaying historical strategy performance.

One option snapshot references exactly one `option_contract`.

Snapshots are normally collected every five minutes while the market is open.

---

### Contract Data

Contract-definition data is stored in `option_contract`, not in `option_snapshot`.

| JSON field | Database location | Relevance |
|---|---|---|
| `details.contract_type` | `option_contract.contract_type` | Defines whether the contract is a CALL or PUT |
| `details.exercise_style` | `option_contract.exercise_style` | Identifies American or European exercise |
| `details.expiration_date` | `option_contract.expiration_date` | Contract expiry |
| `details.shares_per_contract` | `option_contract.shares_per_contract` | Contract multiplier, normally 100 |
| `details.strike_price` | `option_contract.strike_price` | Option strike |
| `details.ticker` | `option_contract.contract_ticker` | Stable external identifier for the contract |

Contract fields should not be duplicated in every snapshot.

---

### Time Fields

The API provides separate timestamps for the option quote, option trade and underlying asset.

The application also creates its own collection timestamp.

| Database field | JSON mapping | Meaning |
|---|---|---|
| `snapshot_time` | Generated by Java application | Time the application collected and stored the complete snapshot |
| `option_quote_time` | `last_quote.last_updated` | Time the bid/ask quote was updated |
| `option_trade_time` | `last_trade.sip_timestamp` | Time the last option trade occurred |
| `underlying_time` | `underlying_asset.last_updated` | Time the underlying stock price was updated |

The provider timestamps are Unix epoch timestamps expressed in nanoseconds.

Example:

```
1783020294485652677
```

Java conversion:

```
long seconds = Math.floorDiv(timestamp, 1_000_000_000L);
long nanos = Math.floorMod(timestamp, 1_000_000_000L);
Instant value = Instant.ofEpochSecond(seconds, nanos);
```

All timestamps should be interpreted and persisted in UTC.

`DATETIME(6)` stores microsecond precision. The final three digits of the provider nanosecond precision are not retained.

---

### Underlying Fields

| Database field | JSON mapping | Relevance |
|---|---|---|
| `underlying_price` | `underlying_asset.price` | Stock price used to calculate moneyness and option exposure |
| `underlying_time` | `underlying_asset.last_updated` | Age of the underlying price |
| `underlying_timeframe` | `underlying_asset.timeframe` | Indicates whether the underlying price is delayed or real-time |
| `change_to_break_even` | `underlying_asset.change_to_break_even` | Underlying movement required to reach option break-even |

The underlying price may have a different timestamp and timeframe from the option quote.

A real-time option quote combined with a delayed underlying price should be identified clearly because Greeks and moneyness calculations may not represent the same market instant.

---

### Break-Even Fields

| Database field | JSON mapping | Relevance |
|---|---|---|
| `break_even_price` | `break_even_price` | Approximate stock price required for the individual option to break even at expiry |
| `change_to_break_even` | `underlying_asset.change_to_break_even` | Point movement from the current stock price to break-even |

Break-even values are useful for display and filtering.

They should not replace full strategy-level break-even calculations for spreads, straddles, strangles or iron condors.

---

### Quote Fields

| Database field | JSON mapping | Relevance |
|---|---|---|
| `bid` | `last_quote.bid` | Highest displayed price currently offered by buyers |
| `ask` | `last_quote.ask` | Lowest displayed price currently offered by sellers |
| `midpoint` | `last_quote.midpoint` | Midpoint between bid and ask |
| `bid_size` | `last_quote.bid_size` | Size available at the bid |
| `ask_size` | `last_quote.ask_size` | Size available at the ask |
| `bid_exchange` | `last_quote.bid_exchange` | Exchange displaying the bid |
| `ask_exchange` | `last_quote.ask_exchange` | Exchange displaying the ask |
| `option_quote_time` | `last_quote.last_updated` | Timestamp of the quote |
| `quote_timeframe` | `last_quote.timeframe` | Delayed or real-time quote |

Quote data is essential for:

- Liquidity analysis.
- Bid/ask spread analysis.
- Entry and exit simulation.
- Detecting stale or poor-quality markets.
- Estimating executable position value.

Derived values include:

```
spread = ask - bid

spread percentage =
    ((ask - bid) / midpoint) x 100
```

For strategy analysis, midpoint may be used as the mark.

For an executable liquidation estimate:

- Use bid when closing a LONG option.
- Use ask when closing a SHORT option.

---

### Last Trade Fields

| Database field | JSON mapping | Relevance |
|---|---|---|
| `last_trade_price` | `last_trade.price` | Price of the most recent option trade |
| `last_trade_size` | `last_trade.size` | Number of contracts in the trade |
| `last_trade_exchange` | `last_trade.exchange` | Exchange where the trade occurred |
| `last_trade_conditions` | `last_trade.conditions` | Provider-specific trade-condition codes |
| `option_trade_time` | `last_trade.sip_timestamp` | Timestamp of the trade |
| `trade_timeframe` | `last_trade.timeframe` | Delayed or real-time trade data |

The last trade price should not automatically be treated as the current option value.

A contract may have a recent quote but an old last trade.

Quote age and trade age should be calculated independently.

---

### Implied Volatility

| Database field | JSON mapping | Relevance |
|---|---|---|
| `implied_volatility` | `implied_volatility` | Market-implied annualized volatility for the option |

The API supplies IV as a decimal.

Example:

```
1.0524735927817275
```

Display value:

```
105.247359%
```

The database should store the decimal value as supplied.

IV is central to the application because it is used for:

- IV-change alerts.
- IV expansion detection.
- IV crush detection.
- Call-versus-put IV comparison.
- Historical IV analysis.
- Entry and exit filtering.
- Vega impact calculations.

Useful derived metrics include:

```
IV change over 5 minutes
IV change over 15 minutes
IV change over 30 minutes
IV change over 1 hour
IV change since strategy entry
Call IV minus Put IV
```

Alerts should normally be based on a combination of absolute and percentage changes.

Example:

```
Absolute IV change:
    current IV - previous IV

Relative IV change:
    ((current IV - previous IV) / previous IV) x 100
```

---

### Greeks

| Database field | JSON mapping | Relevance |
|---|---|---|
| `delta` | `greeks.delta` | Approximate option-price sensitivity to a one-point underlying move |
| `gamma` | `greeks.gamma` | Approximate change in Delta for a one-point underlying move |
| `theta` | `greeks.theta` | Approximate daily time-value decay, all else equal |
| `vega` | `greeks.vega` | Approximate option-price sensitivity to an IV change |

Greeks belong to a point-in-time market snapshot because they change with:

- Underlying price.
- Time remaining.
- Implied volatility.
- Strike moneyness.
- Interest rates.
- Dividend assumptions.

Greeks must not be treated as permanent contract attributes.

The application should compare Greek changes between snapshots.

Examples:

```
Delta change = current Delta - previous Delta
Gamma change = current Gamma - previous Gamma
Theta change = current Theta - previous Theta
Vega change = current Vega - previous Vega
```

Useful ratios include:

```
Gamma-to-Theta ratio = gamma / absolute(theta)
Vega-to-Theta ratio = vega / absolute(theta)
```

These ratios should be used carefully and compared only between contracts with similar underlying prices, expiry and moneyness.

---

### Open Interest

| Database field | JSON mapping | Relevance |
|---|---|---|
| `open_interest` | `open_interest` | Number of outstanding open contracts reported for the option |

Open interest is useful for:

- Identifying heavily positioned strikes.
- Evaluating liquidity.
- Detecting call and put concentration.
- Comparing volume against existing positioning.
- Finding possible support, resistance or pinning zones.

Open interest should not be treated as a true five-minute live counter.

It may be based on the previous clearing cycle.

Therefore:

- Store it with every snapshot.
- Track changes between trading days.
- Do not automatically interpret intraday changes as new live positions.

---

### Day Volume

| Database field | JSON mapping | Relevance |
|---|---|---|
| `day_volume` | `day.volume` | Cumulative option volume for the current trading day |

`day_volume` is cumulative.

It is not the volume traded during the five-minute snapshot interval.

Five-minute volume can be derived as:

```
current day volume - previous snapshot day volume
```

Example:

```
10:00 day volume = 105
10:05 day volume = 140

Five-minute interval volume = 35
```

Useful derived metrics include:

```
Five-minute volume
Fifteen-minute volume
Hourly volume
Volume acceleration
Volume-to-open-interest ratio
Current hourly volume versus historical hourly average
```

A high increase in volume during a specific period may indicate unusual activity.

---

### Timeframe Fields

Supported values:

```
DELAYED
REAL-TIME
```

| Database field | JSON mapping |
|---|---|
| `quote_timeframe` | `last_quote.timeframe` |
| `trade_timeframe` | `last_trade.timeframe` |
| `underlying_timeframe` | `underlying_asset.timeframe` |

Timeframes should be recorded separately.

The option quote may be real-time while the underlying price is delayed.

Such a mismatch should generate a data-quality warning before using the snapshot for precise calculations.

---

### Snapshot Time Versus Provider Time

`snapshot_time` is the application collection time.

Provider timestamps describe when individual parts of the market data were updated.

For one snapshot:

```
snapshot_time:
    10:05:00.000000

option_quote_time:
    10:04:59.312475

option_trade_time:
    09:57:11.660406

underlying_time:
    10:04:54.485652
```

This means:

- The application collected the record at 10:05.
- The option quote was fresh.
- The last trade was several minutes old.
- The underlying price was also recently updated.

Derived freshness metrics should include:

```
Quote age = snapshot_time - option_quote_time
Trade age = snapshot_time - option_trade_time
Underlying age = snapshot_time - underlying_time
```

---

### Data-Quality Rules

A snapshot should be treated with caution when:

- Bid is null or zero.
- Ask is null or zero.
- Ask is less than bid.
- Midpoint is null.
- Quote age exceeds the configured threshold.
- Underlying age exceeds the configured threshold.
- Quote and underlying timeframes differ.
- Greeks are missing.
- IV is missing or non-positive.
- Spread percentage is unusually high.
- Last trade is stale.
- Open interest or volume is inconsistent with prior observations.

The application should calculate a snapshot-quality status such as:

```
VALID
STALE_QUOTE
STALE_UNDERLYING
WIDE_SPREAD
MISSING_GREEKS
MISSING_IV
INVALID_MARKET
TIMEFRAME_MISMATCH
```

This status may initially be derived rather than stored.

---

### JSON-to-Table Mapping Summary

| Table field | JSON path / source | Importance |
|---|---|---|
| `option_contract_id` | Resolved using `details.ticker` | Required foreign key |
| `snapshot_time` | Generated by application | Required collection time |
| `option_quote_time` | `last_quote.last_updated` | Quote freshness |
| `option_trade_time` | `last_trade.sip_timestamp` | Last-trade freshness |
| `underlying_time` | `underlying_asset.last_updated` | Underlying-price freshness |
| `underlying_price` | `underlying_asset.price` | Required for moneyness and valuation |
| `break_even_price` | `break_even_price` | Individual option break-even |
| `change_to_break_even` | `underlying_asset.change_to_break_even` | Distance to break-even |
| `bid` | `last_quote.bid` | Executable sell-side reference |
| `ask` | `last_quote.ask` | Executable buy-side reference |
| `midpoint` | `last_quote.midpoint` | Analytical mark |
| `last_trade_price` | `last_trade.price` | Most recent trade |
| `bid_size` | `last_quote.bid_size` | Bid liquidity |
| `ask_size` | `last_quote.ask_size` | Ask liquidity |
| `last_trade_size` | `last_trade.size` | Trade size |
| `bid_exchange` | `last_quote.bid_exchange` | Bid venue |
| `ask_exchange` | `last_quote.ask_exchange` | Ask venue |
| `last_trade_exchange` | `last_trade.exchange` | Trade venue |
| `last_trade_conditions` | `last_trade.conditions` | Trade metadata |
| `implied_volatility` | `implied_volatility` | Core volatility metric |
| `delta` | `greeks.delta` | Directional sensitivity |
| `gamma` | `greeks.gamma` | Delta acceleration |
| `theta` | `greeks.theta` | Time decay |
| `vega` | `greeks.vega` | IV sensitivity |
| `open_interest` | `open_interest` | Outstanding contracts |
| `day_volume` | `day.volume` | Cumulative daily activity |
| `quote_timeframe` | `last_quote.timeframe` | Quote real-time status |
| `trade_timeframe` | `last_trade.timeframe` | Trade real-time status |
| `underlying_timeframe` | `underlying_asset.timeframe` | Underlying real-time status |

---

### Fields Intentionally Not Stored Initially

The following JSON fields may be omitted from the initial snapshot model:

- `day.change`
- `day.change_percent`
- `day.close`
- `day.high`
- `day.low`
- `day.open`
- `day.previous_close`
- `day.vwap`
- `day.last_updated`

They are option-level daily aggregates.

They can be added later if the application needs:

- Intraday option OHLC analysis.
- Option VWAP comparison.
- Option momentum.
- Daily option-return analysis.

`day.volume` should still be retained because it is required for liquidity and activity metrics.

---

### Core Principle

`option_snapshot` should preserve the raw point-in-time market state.

Derived values such as:

- Moneyness.
- Spread percentage.
- Quote age.
- Five-minute volume.
- IV change.
- Greek changes.
- Gamma-to-Theta ratio.
- Strategy P&L.

should normally be calculated from snapshots rather than replacing the raw source values.