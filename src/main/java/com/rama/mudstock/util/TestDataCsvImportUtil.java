package com.rama.mudstock.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.rama.mudstock.MudServerApplication;

public final class TestDataCsvImportUtil {

    @FunctionalInterface
    private interface CsvRowHandler {
        void handle(List<String> row, Map<String, Integer> headerIndex) throws Exception;
    }

    // Keep CSV file path at the top for quick edits.
    private static final String STOCK_MOVEMENT_CSV_FILE_PATH = "/Users/I753307/Library/Mobile Documents/com~apple~CloudDocs/Tech-util/mock_data/csv/stockmovement_data_mu.csv";
    private static final String OPTION_CONTRACT_CSV_FILE_PATH = "/Users/I753307/Library/Mobile Documents/com~apple~CloudDocs/Tech-util/mock_data/csv/option_contract_mu_expiry31-07-2026.csv";
    private static final String OPTION_SNAPSHOT_CSV_FILE_PATH = "/Users/I753307/Library/Mobile Documents/com~apple~CloudDocs/Tech-util/mock_data/csv/option_snapshot_mu_expiry31-07-2026.csv";
    private static final String OPTION_SNAPSHOT_FLATFILE_CSV_FILE_PATH = "/Users/I753307/Library/Mobile Documents/com~apple~CloudDocs/Tech-util/mock_data/csv/option_snapshot_flatfile_mu_expiry31-07-2026.csv";

    private static final boolean RUN_STOCK_MOVEMENT_IMPORT = false;
    private static final boolean RUN_OPTION_CONTRACT_IMPORT = false;
    private static final boolean RUN_OPTION_SNAPSHOT_IMPORT = false;
    private static final boolean RUN_OPTION_SNAPSHOT_FLATFILE_IMPORT = true;

    private static final String INSERT_SQL = "INSERT INTO day_stock_movement_entry "
        + "(stock_id, earnings_date_id, pre_day_close, cur_day_open, cur_day_close, cur_day_high, "
        + "cur_day_low, cur_day_vol_weight, cur_day_volume, change_percent, earnings, "
        + "day_opening_change_percent, day_stock_movement_date) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OPTION_CONTRACT_SQL = "INSERT INTO option_contract "
        + "(stock_id, options_interval_analyse_id, contract_type, exercise_style, expiration_date, "
        + "strike_price, shares_per_contract, contract_ticker, status, source) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OPTION_SNAPSHOT_SQL = "INSERT INTO option_snapshot "
        + "(option_contract_id, stock_id, snapshot_time, unix_time, option_quote_time, option_trade_time, underlying_time, "
        + "underlying_price, break_even_price, change_to_break_even, bid, ask, midpoint, last_trade_price, "
        + "bid_size, ask_size, last_trade_size, implied_volatility, open_interest, day_volume, "
        + "delta, gamma, theta, vega, quote_timeframe, underlying_timeframe, trade_timeframe, "
        + "bid_exchange, ask_exchange, last_trade_exchange, snapshot_version) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_OPTION_SNAPSHOT_FLATFILE_SQL = "INSERT INTO option_snapshot_flatfile "
        + "(option_contract_id, stock_id, contract_ticker, opt_volume, opt_open, opt_close, opt_high, opt_low, "
        + "unix_time, unix_utc_time, local_time, stock_ticker, stock_open, stock_close, stock_high, stock_low, snapshot_version) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private TestDataCsvImportUtil() {
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(MudServerApplication.class)
            .web(WebApplicationType.NONE)
            .run(args)) {

            JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
            if (RUN_STOCK_MOVEMENT_IMPORT) {
                int stockMovementInserted = importStockMovementDataCsv(jdbcTemplate, Path.of(STOCK_MOVEMENT_CSV_FILE_PATH));
                System.out.println("Inserted rows into day_stock_movement_entry: " + stockMovementInserted);
            }

            if (RUN_OPTION_CONTRACT_IMPORT) {
                int optionContractInserted = importOptionContractCsv(jdbcTemplate, Path.of(OPTION_CONTRACT_CSV_FILE_PATH));
                System.out.println("Inserted rows into option_contract: " + optionContractInserted);
            }

            if (RUN_OPTION_SNAPSHOT_IMPORT) {
                int optionSnapshotInserted = importOptionSnapshotCsv(jdbcTemplate, Path.of(OPTION_SNAPSHOT_CSV_FILE_PATH));
                System.out.println("Inserted rows into option_snapshot: " + optionSnapshotInserted);
            }

            if (RUN_OPTION_SNAPSHOT_FLATFILE_IMPORT) {
                int optionSnapshotFlatfileInserted = importOptionSnapshotFlatfileCsv(jdbcTemplate, Path.of(OPTION_SNAPSHOT_FLATFILE_CSV_FILE_PATH));
                System.out.println("Inserted rows into option_snapshot_flatfile: " + optionSnapshotFlatfileInserted);
            }
        } catch (Exception ex) {
            System.err.println("CSV import failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int importStockMovementDataCsv(JdbcTemplate jdbcTemplate, Path csvPath) throws IOException {
        return importCsvRows(csvPath, (row, headerIndex) -> jdbcTemplate.update(
            INSERT_SQL,
            parseLong(read(row, headerIndex, "stock_id")),
            parseNullableLong(read(row, headerIndex, "earnings_date_id")),
            parseDecimal(read(row, headerIndex, "pre_day_close")),
            parseDecimal(read(row, headerIndex, "cur_day_open")),
            parseDecimal(read(row, headerIndex, "cur_day_close")),
            parseDecimal(read(row, headerIndex, "cur_day_high")),
            parseDecimal(read(row, headerIndex, "cur_day_low")),
            parseDecimal(read(row, headerIndex, "cur_day_vol_weight")),
            parseLong(read(row, headerIndex, "cur_day_volume")),
            parseNullableDecimal(read(row, headerIndex, "change_percent")),
            parseBoolean(read(row, headerIndex, "earnings")),
            parseNullableDecimal(read(row, headerIndex, "day_opening_change_percent")),
            parseTimestamp(read(row, headerIndex, "day_stock_movement_date"))
        ));
    }

    private static int importOptionContractCsv(JdbcTemplate jdbcTemplate, Path csvPath) throws IOException {
        return importCsvRows(csvPath, (row, headerIndex) -> jdbcTemplate.update(
            INSERT_OPTION_CONTRACT_SQL,
            parseLong(read(row, headerIndex, "stock_id")),
            parseNullableLong(read(row, headerIndex, "options_interval_analyse_id")),
            parseUpper(read(row, headerIndex, "contract_type")),
            parseUpper(read(row, headerIndex, "exercise_style")),
            parseSqlDate(read(row, headerIndex, "expiration_date")),
            parseDecimal(read(row, headerIndex, "strike_price")),
            parseInteger(read(row, headerIndex, "shares_per_contract")),
            parseNullableString(read(row, headerIndex, "contract_ticker")),
            parseUpper(read(row, headerIndex, "status")),
            parseUpper(read(row, headerIndex, "source"))
        ));
    }

    private static int importOptionSnapshotCsv(JdbcTemplate jdbcTemplate, Path csvPath) throws IOException {
        Map<String, Long> contractIdCache = new HashMap<>();
        return importCsvRows(csvPath, (row, headerIndex) -> {
            String contractTicker = parseNullableString(read(row, headerIndex, "contract_ticker"));
            if (contractTicker == null) {
                throw new IllegalArgumentException("contract_ticker is required");
            }

            Long optionContractId = resolveOptionContractId(jdbcTemplate, contractIdCache, contractTicker);
            if (optionContractId == null) {
                throw new IllegalArgumentException("No option_contract found for contract_ticker=" + contractTicker);
            }

            jdbcTemplate.update(
                INSERT_OPTION_SNAPSHOT_SQL,
                optionContractId,
                parseLong(read(row, headerIndex, "stock_id")),
                parseTimestamp(read(row, headerIndex, "snapshot_time")),
                parseLong(read(row, headerIndex, "unix_time")),
                parseNullableTimestamp(read(row, headerIndex, "option_quote_time")),
                parseNullableTimestamp(read(row, headerIndex, "option_trade_time")),
                parseNullableTimestamp(read(row, headerIndex, "underlying_time")),
                parseDecimal(read(row, headerIndex, "underlying_price")),
                parseNullableDecimal(read(row, headerIndex, "break_even_price")),
                parseNullableDecimal(read(row, headerIndex, "change_to_break_even")),
                parseNullableDecimal(read(row, headerIndex, "bid")),
                parseNullableDecimal(read(row, headerIndex, "ask")),
                parseNullableDecimal(read(row, headerIndex, "midpoint")),
                parseNullableDecimal(read(row, headerIndex, "last_trade_price")),
                parseNullableInteger(read(row, headerIndex, "bid_size")),
                parseNullableInteger(read(row, headerIndex, "ask_size")),
                parseNullableInteger(read(row, headerIndex, "last_trade_size")),
                parseNullableDecimal(read(row, headerIndex, "implied_volatility")),
                parseNullableInteger(read(row, headerIndex, "open_interest")),
                parseNullableInteger(read(row, headerIndex, "day_volume")),
                parseNullableDecimal(read(row, headerIndex, "delta")),
                parseNullableDecimal(read(row, headerIndex, "gamma")),
                parseNullableDecimal(read(row, headerIndex, "theta")),
                parseNullableDecimal(read(row, headerIndex, "vega")),
                parseUpper(read(row, headerIndex, "quote_timeframe")),
                parseUpper(read(row, headerIndex, "underlying_timeframe")),
                parseUpper(read(row, headerIndex, "trade_timeframe")),
                parseNullableInteger(read(row, headerIndex, "bid_exchange")),
                parseNullableInteger(read(row, headerIndex, "ask_exchange")),
                parseNullableInteger(read(row, headerIndex, "last_trade_exchange")),
                parseLong(read(row, headerIndex, "snapshot_version"))
            );
        });
    }

    private static int importOptionSnapshotFlatfileCsv(JdbcTemplate jdbcTemplate, Path csvPath) throws IOException {
        Map<String, Long> contractIdCache = new HashMap<>();
        return importCsvRows(csvPath, (row, headerIndex) -> {
            String contractTicker = parseNullableString(read(row, headerIndex, "contract_ticker"));
            if (contractTicker == null) {
                throw new IllegalArgumentException("contract_ticker is required");
            }

            Long optionContractId = resolveOptionContractId(jdbcTemplate, contractIdCache, contractTicker);
            if (optionContractId == null) {
                throw new IllegalArgumentException("No option_contract found for contract_ticker=" + contractTicker);
            }

            jdbcTemplate.update(
                INSERT_OPTION_SNAPSHOT_FLATFILE_SQL,
                optionContractId,
                parseLong(read(row, headerIndex, "stock_id")),
                contractTicker,
                parseNullableInteger(read(row, headerIndex, "opt_volume")),
                parseNullableDecimal(read(row, headerIndex, "opt_open")),
                parseNullableDecimal(read(row, headerIndex, "opt_close")),
                parseNullableDecimal(read(row, headerIndex, "opt_high")),
                parseNullableDecimal(read(row, headerIndex, "opt_low")),
                parseLong(read(row, headerIndex, "unix_time")),
                parseTimestamp(read(row, headerIndex, "unix_utc_time")),
                parseTimestamp(read(row, headerIndex, "local_time")),
                parseNullableString(read(row, headerIndex, "stock_ticker")),
                parseNullableDecimal(read(row, headerIndex, "stock_open")),
                parseNullableDecimal(read(row, headerIndex, "stock_close")),
                parseNullableDecimal(read(row, headerIndex, "stock_high")),
                parseNullableDecimal(read(row, headerIndex, "stock_low")),
                parseLong(read(row, headerIndex, "snapshot_version"))
            );
        });
    }

    private static int importCsvRows(Path csvPath, CsvRowHandler rowHandler) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException("CSV file not found: " + csvPath);
        }

        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return 0;
        }

        List<String> headerColumns = parseCsvLine(lines.get(0));
        Map<String, Integer> headerIndex = buildHeaderIndex(headerColumns);

        int inserted = 0;
        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }

            List<String> row = parseCsvLine(raw);
            try {
                rowHandler.handle(row, headerIndex);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Failed to process CSV row " + (i + 1) + ": " + ex.getMessage(), ex);
            }
            inserted++;
        }

        return inserted;
    }

    private static Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(normalizeHeader(headers.get(i)), i);
        }
        return index;
    }

    private static String read(List<String> row, Map<String, Integer> headerIndex, String column) {
        Integer idx = headerIndex.get(normalizeHeader(column));
        if (idx == null || idx < 0 || idx >= row.size()) {
            throw new IllegalArgumentException("Missing column in CSV header: " + column);
        }
        return normalizeCsvValue(row.get(idx));
    }

    private static String normalizeCsvValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if ("NULL".equalsIgnoreCase(value)) {
            return "";
        }
        return value;
    }

    private static String normalizeHeader(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private static List<String> parseCsvLine(String line) {
        char delimiter = detectDelimiter(line);
        return parseCsvLine(line, delimiter);
    }

    private static List<String> parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());

        return values;
    }

    private static char detectDelimiter(String line) {
        int commaCount = 0;
        int semicolonCount = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                continue;
            }
            if (ch == ',') {
                commaCount++;
            } else if (ch == ';') {
                semicolonCount++;
            }
        }
        return semicolonCount > commaCount ? ';' : ',';
    }

    private static long parseLong(String raw) {
        Long value = parseNullableLong(raw);
        if (value == null) {
            throw new IllegalArgumentException("Required numeric value is empty");
        }
        return value;
    }

    private static Long parseNullableLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Long.valueOf(raw.trim());
    }

    private static BigDecimal parseDecimal(String raw) {
        BigDecimal value = parseNullableDecimal(raw);
        if (value == null) {
            throw new IllegalArgumentException("Required decimal value is empty");
        }
        return value;
    }

    private static BigDecimal parseNullableDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return new BigDecimal(raw.trim());
    }

    private static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Required integer value is empty");
        }
        return Integer.valueOf(raw.trim());
    }

    private static Integer parseNullableInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Integer.valueOf(raw.trim());
    }

    private static String parseNullableString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private static String parseUpper(String raw) {
        String value = parseNullableString(raw);
        if (value == null) {
            throw new IllegalArgumentException("Required string value is empty");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "y".equals(normalized);
    }

    private static Timestamp parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Required timestamp value is empty");
        }

        Timestamp parsed = parseTimestampInternal(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Unsupported timestamp format: " + raw);
        }
        return parsed;
    }

    private static Timestamp parseNullableTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        Timestamp parsed = parseTimestampInternal(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Unsupported timestamp format: " + raw);
        }
        return parsed;
    }

    private static Timestamp parseTimestampInternal(String raw) {
        String value = raw.trim();

        try {
            return Timestamp.valueOf(value.replace('T', ' '));
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Timestamp.valueOf(LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Timestamp.valueOf(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay());
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private static Date parseSqlDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("expiration_date is required");
        }

        String value = raw.trim();
        try {
            return Date.valueOf(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Date.valueOf(LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Date.valueOf(LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException("Unsupported expiration_date format: " + raw);
    }

    private static Long resolveOptionContractId(JdbcTemplate jdbcTemplate,
                                                Map<String, Long> contractIdCache,
                                                String contractTicker) {
        String normalized = contractTicker.trim().toUpperCase(Locale.ROOT);
        if (contractIdCache.containsKey(normalized)) {
            return contractIdCache.get(normalized);
        }

        String sql = "SELECT id FROM option_contract WHERE UPPER(contract_ticker) = UPPER(?) ORDER BY id DESC LIMIT 1";
        List<Long> rows = jdbcTemplate.queryForList(sql, Long.class, normalized);
        Long resolved = rows.isEmpty() ? null : rows.get(0);
        contractIdCache.put(normalized, resolved);
        return resolved;
    }
}