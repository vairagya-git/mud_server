package com.rama.mudstock.util;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TypeConverstionUtilTest {

    @Test
    void portugalTimestamp_isOneHourAhead_duringDST() {
        Instant instant = Instant.parse("2026-07-14T18:45:00Z"); // WEST (UTC+1)
        long epochNanos = instant.getEpochSecond() * 1_000_000_000L + instant.getNano();

        Timestamp utcTs = TypeConverstionUtil.toTimestampFromEpochNanos(epochNanos);
        Timestamp lisbonTs = TypeConverstionUtil.toPortugalTimestampFromEpochNanos(epochNanos);

        long diffMillis = lisbonTs.getTime() - utcTs.getTime();
        assertEquals(3_600_000L, diffMillis, "Lisbon time should be 1 hour ahead of UTC during DST (July)");
    }

    @Test
    void portugalTimestamp_matchesUtc_outsideDST() {
        Instant instant = Instant.parse("2026-01-14T18:45:00Z"); // WET (UTC+0)
        long epochNanos = instant.getEpochSecond() * 1_000_000_000L + instant.getNano();

        Timestamp utcTs = TypeConverstionUtil.toTimestampFromEpochNanos(epochNanos);
        Timestamp lisbonTs = TypeConverstionUtil.toPortugalTimestampFromEpochNanos(epochNanos);

        long diffMillis = lisbonTs.getTime() - utcTs.getTime();
        assertEquals(0L, diffMillis, "Lisbon time should equal UTC outside DST (January)");
    }
}
