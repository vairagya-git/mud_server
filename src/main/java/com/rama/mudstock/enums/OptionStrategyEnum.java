package com.rama.mudstock.enums;

public final class OptionStrategyEnum {

    private OptionStrategyEnum() {
    }

    public enum StrategyType {
        LONG_CALL,
        LONG_PUT,
        LONG_STRADDLE,
        LONG_STRANGLE,
        BULL_CALL_SPREAD,
        BEAR_CALL_SPREAD,
        BULL_PUT_SPREAD,
        BEAR_PUT_SPREAD,
        IRON_CONDOR,
        IRON_BUTTERFLY,
        REVERSE_IRON_CONDOR,
        CUSTOM
    }

    public enum StrategyMode {
        LIVE_TRADE,
        SIMULATION
    }

    public enum StrategyAction {
        OPEN,
        ADJUST,
        ROLL,
        CLOSE
    }

    public enum StrategyStatus {
        OPEN,
        CLOSED,
        CANCELLED
    }
}