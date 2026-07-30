package com.rama.mudstock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central date utility for the mud-server project.
 *
 * <p>All date formatting patterns and parsing logic are consolidated here so
 * that no inline {@link DateTimeFormatter} literals or hand-rolled pattern
 * checks are scattered across controllers, services, and schedulers.</p>
 *
 * <p>All methods are {@code static} — no instantiation needed.</p>
 */
public final class MudDateUtil {

    private static final Pattern OPTION_CONTRACT_PATTERN =
        Pattern.compile("^O:([A-Z0-9]+)(\\d{6})([PC])(\\d{8})$");
    private static final String OPTION_CONTRACT_TEMPLATE = "O:{TICKER}{EXPIRY}{TYPE}{STRIKE}";

    // -----------------------------------------------------------------------
    // Formatters (package-private constants so tests can reference them)
    // -----------------------------------------------------------------------

    /** ISO date: {@code yyyy-MM-dd} */
    public static final DateTimeFormatter FMT_ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Slash date: {@code dd/MM/yyyy} */
    public static final DateTimeFormatter FMT_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Date-time to minute precision: {@code yyyy-MM-dd HH:mm}. */
    public static final DateTimeFormatter FMT_DATE_TIME_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Day-code format used for auto-generated movement key codes: {@code dd_MMM_yy}
     * (e.g. {@code 01_JUN_26}).  Results must be uppercased by the caller.
     */
    public static final DateTimeFormatter FMT_DAY_CODE =
            DateTimeFormatter.ofPattern("dd_MMM_yy", java.util.Locale.ENGLISH);

    /**
     * Earnings bulk-upload date format: {@code d-M-yyyy}
     * (e.g. {@code 15-7-2026} or {@code 5-1-2026}).
     */
    public static final DateTimeFormatter FMT_D_M_YYYY =
            DateTimeFormatter.ofPattern("d-M-yyyy");

    /** ISO pattern regex — used to decide which formatter to apply. */
    private static final String PATTERN_ISO   = "\\d{4}-\\d{2}-\\d{2}";

    /** Slash pattern regex — {@code dd/MM/yyyy}. */
    private static final String PATTERN_SLASH = "\\d{2}/\\d{2}/\\d{4}";

    private MudDateUtil() { /* utility class — no instances */ }

    // -----------------------------------------------------------------------
    // Formatting
    // -----------------------------------------------------------------------

    /**
     * Formats a {@link LocalDate} as {@code yyyy-MM-dd}.
     *
     * @param date the date to format; {@code null} returns {@code ""}
     * @return formatted string, never {@code null}
     */
    public static String toIsoString(LocalDate date) {
        if (date == null) return "";
        return date.format(FMT_ISO);
    }

    /**
     * Formats a {@link LocalDate} as {@code dd/MM/yyyy}.
     *
     * @param date the date to format; {@code null} returns {@code ""}
     * @return formatted string, never {@code null}
     */
    public static String toSlashString(LocalDate date) {
        if (date == null) return "";
        return date.format(FMT_SLASH);
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a date string that may be either {@code yyyy-MM-dd} or
     * {@code dd/MM/yyyy}.
     *
     * <p>Leading/trailing whitespace and surrounding double-quotes are
     * stripped automatically before parsing.</p>
     *
     * @param raw the raw string (may be {@code null} or blank)
     * @return parsed {@link LocalDate}, or {@code null} if the input is
     *         blank or does not match either pattern
     */
    public static LocalDate parseFlexible(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().replaceAll("^\"|\"$", "");
        try {
            if (isIsoPattern(s))   return LocalDate.parse(s, FMT_ISO);
            if (isSlashPattern(s)) return LocalDate.parse(s, FMT_SLASH);
        } catch (DateTimeParseException ignored) { /* fall through */ }
        return null;
    }

    /**
     * Parses a strict {@code yyyy-MM-dd} string.
     *
     * @param raw the ISO date string
     * @return parsed {@link LocalDate}
     * @throws DateTimeParseException if the format does not match
     */
    public static LocalDate parseIso(String raw) {
        return LocalDate.parse(raw.trim(), FMT_ISO);
    }

    /**
     * Parses a strict {@code dd/MM/yyyy} string.
     *
     * @param raw the slash date string
     * @return parsed {@link LocalDate}
     * @throws DateTimeParseException if the format does not match
     */
    public static LocalDate parseSlash(String raw) {
        return LocalDate.parse(raw.trim(), FMT_SLASH);
    }

    // -----------------------------------------------------------------------
    // Pattern checks
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code s} looks like {@code yyyy-MM-dd}.
     */
    public static boolean isIsoPattern(String s) {
        return s != null && s.matches(PATTERN_ISO);
    }

    /**
     * Returns {@code true} when {@code s} looks like {@code dd/MM/yyyy}.
     */
    public static boolean isSlashPattern(String s) {
        return s != null && s.matches(PATTERN_SLASH);
    }

    /**
     * Returns {@code true} when {@code s} matches either the ISO or slash
     * date pattern.
     */
    public static boolean isValidDateString(String s) {
        return isIsoPattern(s) || isSlashPattern(s);
    }

    // -----------------------------------------------------------------------
    // Convenience
    // -----------------------------------------------------------------------

    /**
     * Strips surrounding double-quotes from a string token (e.g. CSV values).
     *
     * @param s input string; may be {@code null}
     * @return trimmed, unquoted string, or {@code ""} if input was {@code null}
     */
    public static String unquote(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("^\"|\"$", "");
    }

    /**
     * Converts a UTC date-time value to a local-time display string
     * ({@code yyyy-MM-dd HH:mm}) using the JVM default time zone.
     *
     * <p>Accepted inputs: {@link Timestamp}, {@link Instant}, and
     * {@link LocalDateTime} (treated as UTC).</p>
     *
     * @param utcValue UTC value from persistence/query layer
     * @return local-time formatted string, or {@code ""} when value is null
     */
    public static String utcToLocalDateTimeMinuteString(Object utcValue) {
        if (utcValue == null) {
            return "";
        }

        Instant instant;
        if (utcValue instanceof Timestamp timestamp) {
            instant = timestamp.toInstant();
        } else if (utcValue instanceof Instant parsedInstant) {
            instant = parsedInstant;
        } else if (utcValue instanceof LocalDateTime localDateTime) {
            instant = localDateTime.atZone(ZoneOffset.UTC).toInstant();
        } else {
            return utcValue.toString();
        }

        return instant
            .atZone(ZoneId.systemDefault())
            .format(FMT_DATE_TIME_MINUTE);
    }

    /**
     * Builds an OCC-style option contract ticker in the format:
     * O:{TICKER}{YYMMDD}{P|C}{STRIKE_8_DIGITS}
     *
     * Example: ticker=INTC, expiryYyMmDd=260731, putCall=P, strike=50
     * returns O:INTC260731P00050000
     */
    public static String buildOptionContractTicker(String ticker,
                                                   String expiryYyMmDd,
                                                   String putCall,
                                                   BigDecimal strike) {
        String normalizedTicker = ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT);
        if (normalizedTicker.isEmpty()) {
            throw new IllegalArgumentException("ticker is required");
        }

        String normalizedExpiry = expiryYyMmDd == null ? "" : expiryYyMmDd.trim();
        if (!normalizedExpiry.matches("\\d{6}")) {
            throw new IllegalArgumentException("expiryYyMmDd must be in YYMMDD format");
        }

        String normalizedType = putCall == null ? "" : putCall.trim().toUpperCase(Locale.ROOT);
        if (!("P".equals(normalizedType) || "C".equals(normalizedType))) {
            throw new IllegalArgumentException("putCall must be either 'P' or 'C'");
        }

        if (strike == null || strike.signum() < 0) {
            throw new IllegalArgumentException("strike must be a non-negative number");
        }

        long strikeScaled = strike.movePointRight(3)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

        return OPTION_CONTRACT_TEMPLATE
            .replace("{TICKER}", normalizedTicker)
            .replace("{EXPIRY}", normalizedExpiry)
            .replace("{TYPE}", normalizedType)
            .replace("{STRIKE}", String.format("%08d", strikeScaled));
    }

    public static String buildOptionContractTicker(String ticker,
                                                   String expiryYyMmDd,
                                                   String putCall,
                                                   double strike) {
        return buildOptionContractTicker(ticker, expiryYyMmDd, putCall, BigDecimal.valueOf(strike));
    }

    /**
     * Extracts ticker from OCC-style contract ticker.
     * Example: O:ASML260731C01640000 -> ASML
     */
    public static String parseOptionContractTicker(String contract) {
        return parseOptionContract(contract).group(1);
    }

    /**
     * Extracts expiry in YYMMDD from OCC-style contract ticker.
     * Example: O:ASML260731C01640000 -> 260731
     */
    public static String parseOptionContractExpiryYyMmDd(String contract) {
        return parseOptionContract(contract).group(2);
    }

    /**
     * Extracts option type (P or C) from OCC-style contract ticker.
     * Example: O:ASML260731P00880000 -> P
     */
    public static String parseOptionContractType(String contract) {
        return parseOptionContract(contract).group(3);
    }

    /**
     * Extracts strike from OCC-style contract ticker.
     * Example: O:ASML260731C01640000 -> 1640
     */
    public static BigDecimal parseOptionContractStrike(String contract) {
        String encodedStrike = parseOptionContract(contract).group(4);
        return new BigDecimal(encodedStrike).movePointLeft(3).stripTrailingZeros();
    }

    private static Matcher parseOptionContract(String contract) {
        String normalized = contract == null ? "" : contract.trim().toUpperCase(Locale.ROOT);
        Matcher matcher = OPTION_CONTRACT_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid option contract format: " + contract);
        }
        return matcher;
    }
}