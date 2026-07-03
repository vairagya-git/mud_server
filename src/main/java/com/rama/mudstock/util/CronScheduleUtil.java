package com.rama.mudstock.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.scheduling.support.CronExpression;

public final class CronScheduleUtil {

    private CronScheduleUtil() {
    }

    public static boolean shouldExecuteNow(String rawCronExpression, ZoneId zoneId) {
        LocalDateTime now = LocalDateTime.now(zoneId);
        return shouldExecuteNow(rawCronExpression, zoneId, now);
    }

    public static boolean shouldExecuteSinceLastUpdated(String rawCronExpression,
                                                        String rawLastUpdated,
                                                        ZoneId zoneId) {
        String cronExpression = normalizeCronExpression(rawCronExpression);
        if (cronExpression.isBlank()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(zoneId).withSecond(0).withNano(0);
        LocalDateTime reference = parseLastUpdatedToZone(rawLastUpdated, zoneId)
            .map(dt -> dt.withSecond(0).withNano(0))
            .orElse(now.minusMinutes(1));

        if (!reference.isBefore(now)) {
            return false;
        }

        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime nextExecution = cron.next(reference);
            return nextExecution != null && !nextExecution.isAfter(now);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    static boolean shouldExecuteNow(String rawCronExpression, ZoneId zoneId, LocalDateTime now) {
        String cronExpression = normalizeCronExpression(rawCronExpression);
        if (cronExpression.isBlank()) {
            return false;
        }

        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime currentMinute = now.withSecond(0).withNano(0);
            LocalDateTime previousMinute = currentMinute.minusMinutes(1);
            LocalDateTime nextExecution = cron.next(previousMinute);
            return currentMinute.equals(nextExecution);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static String normalizeCronExpression(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        while (value.startsWith("\"")) {
            value = value.substring(1).trim();
        }
        while (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private static java.util.Optional<LocalDateTime> parseLastUpdatedToZone(String rawLastUpdated, ZoneId zoneId) {
        if (rawLastUpdated == null || rawLastUpdated.isBlank()) {
            return java.util.Optional.empty();
        }

        String value = rawLastUpdated.trim();
        try {
            Instant instant = Instant.parse(value);
            return java.util.Optional.of(LocalDateTime.ofInstant(instant, zoneId));
        } catch (Exception ignored) {
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(value);
            return java.util.Optional.of(odt.atZoneSameInstant(zoneId).toLocalDateTime());
        } catch (Exception ignored) {
        }

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(value);
            return java.util.Optional.of(zdt.withZoneSameInstant(zoneId).toLocalDateTime());
        } catch (Exception ignored) {
        }

        try {
            return java.util.Optional.of(LocalDateTime.parse(value));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }
}