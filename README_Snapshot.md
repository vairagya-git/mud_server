# Database Design

The application is built around a **historical time-series model**. Instead of storing only the current state of an options strategy, every market snapshot is preserved, allowing complete replay, backtesting, analytics, and AI-driven learning.

---

# Tables Overview

| Table | Description |
|--------|-------------|
| **option_contract** | Stores the immutable definition of an option contract. |
| **option_snapshot** | Stores market data and Greeks for an option contract at a specific point in time. |
| **option_strategy** | Represents a complete options trading strategy from entry to exit. |
| **option_strategy_leg** | Stores the individual option contracts that make up a strategy. |
| **option_strategy_snapshot** | Stores the overall state of a strategy at periodic intervals (typically every 5 minutes). |
| **option_strategy_leg_snapshot** | Stores the state of each individual strategy leg during every strategy snapshot. |

---

# Table Details

## option_contract

### Purpose

Stores the immutable definition of an option contract.

Each contract is created only once and can be reused by multiple trading strategies.

### Stores

- Underlying stock
- Contract type (Call / Put)
- Strike price
- Expiration date
- Exercise style
- Shares per contract
- Contract ticker

### Notes

This table contains only static contract information and never stores market prices or Greeks.

---

## option_snapshot

### Purpose

Stores the market state of an option contract at a specific point in time.

Snapshots are typically collected every five minutes and form the historical timeline of every option contract.

### Stores

**Market Data**

- Bid
- Ask
- Midpoint
- Last Trade Price

**Underlying Data**

- Underlying Price
- Break-even Price
- Distance to Break-even

**Greeks**

- Delta
- Gamma
- Theta
- Vega
- Implied Volatility

**Market Activity**

- Open Interest
- Daily Volume
- Quote / Trade timestamps
- Exchange information

### Notes

This table provides the historical option data used for replay, simulation, and analytics.

---

## option_strategy

### Purpose

Represents a complete trading strategy.

A strategy groups together one or more option legs and represents the overall trade from entry until closure.

### Examples

- Long Call
- Long Put
- Long Straddle
- Long Strangle
- Bull Call Spread
- Bear Put Spread
- Iron Condor
- Reverse Iron Condor

### Stores

- Strategy Type
- Live Trade / Simulation
- Current Status
- Entry Time
- Exit Time
- Entry Underlying Price
- Exit Underlying Price
- Realized Profit / Loss
- Previous Strategy (used for rolling)

### Notes

This table acts as the parent record for the complete trade.

---

## option_strategy_leg

### Purpose

Stores the individual option contracts that make up a strategy.

Each strategy can contain one or more legs depending on the selected strategy type.

### Examples

**Long Strangle**

- Long Call
- Long Put

**Iron Condor**

- Short Put
- Long Put
- Short Call
- Long Call

### Stores

- Option Contract
- Long / Short Position
- Quantity
- Entry Snapshot
- Exit Snapshot
- Entry Price
- Exit Price
- Realized Profit / Loss

### Notes

By storing the entry and exit snapshots, the exact market conditions at execution time are permanently preserved.

---

## option_strategy_snapshot

### Purpose

Stores the overall valuation of an active strategy at regular intervals.

Snapshots are normally generated every five minutes while the strategy remains open.

### Stores

**Portfolio Value**

- Entry Cost
- Current Market Value
- Unrealized Profit / Loss
- Realized Profit / Loss
- Total Profit / Loss

**Portfolio Greeks**

- Net Delta
- Net Gamma
- Net Theta
- Net Vega

**Market Statistics**

- Average Implied Volatility
- Total Open Interest
- Total Volume

**Underlying**

- Current Underlying Price

### Notes

This table provides a complete historical replay of the strategy throughout its lifetime.

---

## option_strategy_leg_snapshot

### Purpose

Stores the valuation of every individual strategy leg during each strategy snapshot.

Each record references the exact `option_snapshot` used to calculate the leg value.

### Stores

- Current Market Value
- Unrealized Profit / Loss
- Unrealized Profit / Loss %
- Referenced Option Snapshot

### Notes

This table enables detailed analysis of:

- Individual leg performance
- Profit contribution
- Delta evolution
- IV expansion and contraction
- Rolling effectiveness
- Strategy rebalancing

---

# Design Philosophy

The database is designed around **historical replay** rather than simply recording trades.

By combining:

- immutable option contracts,
- historical option market snapshots,
- strategy definitions,
- strategy-level snapshots, and
- leg-level snapshots,

the application can reconstruct every strategy exactly as it evolved over time.

This architecture provides the foundation for:

- Historical replay
- Backtesting
- Strategy comparison
- Rolling and rebalancing analysis
- Performance dashboards
- AI-driven trade analytics
- Statistical validation
- Machine learning and pattern recognition

The design is extensible and allows additional option strategies to be introduced without changing the underlying architecture.