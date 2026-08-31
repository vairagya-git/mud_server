package com.rama.mudstock.util;

public final class AppUtil {

    private static final long BILLION = 1_000_000_000L;
    private static final long MILLION = 1_000_000L;
    private static final long THOUSAND = 1_000L;

    private AppUtil() {
    }

    public static long addMinutesToEpoch(long epochValue, int minutes) {
        long unitFactor = detectEpochUnitFactor(epochValue);
        long deltaInSeconds = (long) minutes * 60L;
        return epochValue + deltaInSeconds * unitFactor;
    }

    public static long secondsToEpochUnit(long seconds, long epochReference) {
        return seconds * detectEpochUnitFactor(epochReference);
    }

    private static long detectEpochUnitFactor(long epochValue) {
        long absoluteValue = Math.abs(epochValue);
        if (absoluteValue >= 1_000_000_000_000_000_000L) {
            return BILLION;
        }
        if (absoluteValue >= 1_000_000_000_000_000L) {
            return MILLION;
        }
        if (absoluteValue >= 1_000_000_000_000L) {
            return THOUSAND;
        }
        return 1L;
    }
}
