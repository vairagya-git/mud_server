package com.rama.mudstock.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.constant.SystemConfigEnum;
import com.rama.mudstock.model.daystock.DayStockMovementKey;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.model.stockwatchlist.Watchlist;
import com.rama.mudstock.repository.daystock.DayStockMovementMapRepository;
import com.rama.mudstock.repository.daystock.DayStockMovementKeyRepository;
import com.rama.mudstock.repository.stockwatchlist.WatchlistRepository;
import com.rama.mudstock.util.MudDateUtil;

/**
 * Shared logic for the "every day event" feature: for a given date, create a single
 * day_stock_movement_key entry for the configured watchlist-codes (from system_config) and
 * map every stock in that watchlist into day_stock_movement_map.
 *
 * Used by both the scheduled job (for today) and the manual date form.
 */
@Service
public class DayStockMovementService {
    private final Logger log = LoggerFactory.getLogger(DayStockMovementService.class);
    private static final ZoneId LISBON = ZoneId.of("Europe/Lisbon");

    private final WatchlistRepository watchlistRepo;
    private final DayStockMovementKeyRepository masterRepo;
    private final DayStockMovementMapRepository mappingRepo;
    private final SystemConfigService systemConfigService;
    private final MarketCalendarService marketCalendarService;

    public DayStockMovementService(WatchlistRepository watchlistRepo, DayStockMovementKeyRepository masterRepo,
                                DayStockMovementMapRepository mappingRepo,
                                SystemConfigService systemConfigService,
                                MarketCalendarService marketCalendarService) {
        this.watchlistRepo = watchlistRepo;
        this.masterRepo = masterRepo;
        this.mappingRepo = mappingRepo;
        this.systemConfigService = systemConfigService;
        this.marketCalendarService = marketCalendarService;
    }

    public String getWatchlistCode() {
        return String.join(",", getWatchlistCodes());
    }

    public List<String> getWatchlistCodes() {
        var watchlistCfg = SystemConfigEnum.DayStockMovementKeyMapEntry.WATCHLIST_CODES;
        return systemConfigService
                .findByPurposeAndCode(watchlistCfg.purpose(), watchlistCfg.code())
                .filter(List.class::isInstance)
                .map(v -> ((List<?>) v).stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .map(String::toUpperCase)
                        .distinct()
                        .toList())
                .orElse(Collections.emptyList());
    }

    /**
     * Create (or reuse) the day_stock_movement_key for the given date and map all stocks in the
     * configured watchlist. Runs in a transaction so the lazy watchlist.stocks collection can
     * be read even when called outside a web request (e.g. from the scheduler thread).
     */
    @Transactional
    public Result populateForDate(LocalDate date) {
        KeyPreparationResult preparation = prepareDayStockMovementKeys(date);
        return createMappingsForPreparedKeys(date, preparation);
    }

    /**
     * Step 1: Prepare keys for the target date.
     * - Validates market-open day (not weekend/holiday)
     * - Creates missing day_stock_movement_key rows or reuses existing ones
     */
    @Transactional
    public KeyPreparationResult prepareDayStockMovementKeys(LocalDate date) {
        List<String> watchlistCodes = getWatchlistCodes();
        if (watchlistCodes.isEmpty()) {
            log.warn("watchlist-codes is not configured in system_config; skipping key preparation");
            return new KeyPreparationResult(Collections.emptyList(), "", Collections.emptyList(), false);
        }

        LocalDate startDate = resolveBackfillStartDate(date);
        if (startDate.isAfter(date)) {
            return new KeyPreparationResult(Collections.emptyList(), String.join(",", watchlistCodes), Collections.emptyList(), false);
        }

        List<String> processedWatchlists = new ArrayList<>();
        List<PreparedKey> preparedKeys = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(date); d = d.plusDays(1)) {
            if (marketCalendarService.isMarketClosed(d)) {
                continue;
            }

            String dayPart = d.format(MudDateUtil.FMT_DAY_CODE).toUpperCase(); // e.g. 01_JUN_26
            for (String watchlistCode : watchlistCodes) {
                processedWatchlists.add(watchlistCode);
                String code = String.format("%s_%s", dayPart, watchlistCode);
                DayStockMovementKey master = getOrCreateDayStockMovementKey(code, watchlistCode, d);
                preparedKeys.add(new PreparedKey(watchlistCode, master));
            }
        }

        String watchlistCodeSummary = String.join(",", processedWatchlists.stream().distinct().toList());
        log.info("prepareDayStockMovementKeys({}): prepared {} key(s) for watchlist-codes [{}]",
                date, preparedKeys.size(), watchlistCodeSummary);

        return new KeyPreparationResult(preparedKeys, watchlistCodeSummary, Collections.emptyList(), false);
    }

    /**
     * Step 2: Create stock-to-key mappings for the prepared keys.
     */
    @Transactional
    public Result createMappingsForPreparedKeys(LocalDate date, KeyPreparationResult preparation) {
        if (preparation == null || preparation.marketClosed()) {
            return new Result(null, "", 0, 0, false);
        }

        List<PreparedKey> preparedKeys = preparation.preparedKeys();
        if (preparedKeys.isEmpty()) {
            return new Result(null, preparation.watchlistCodeSummary(), 0, 0, false);
        }

        String lastMasterCode = null;
        int created = 0;
        int total = 0;
        List<String> missingWatchlists = new ArrayList<>();

        for (PreparedKey prepared : preparedKeys) {
            String watchlistCode = prepared.watchlistCode();
            DayStockMovementKey master = prepared.master();
            lastMasterCode = master.getCode();

            var maybe = watchlistRepo.findByCode(watchlistCode);
            if (maybe.isEmpty()) {
                missingWatchlists.add(watchlistCode);
                log.warn("createMappingsForPreparedKeys({}): watchlist with code '{}' not found; skipping mappings for key={}",
                    date, watchlistCode, master.getCode());
                continue;
            }
            Watchlist watchlist = maybe.get();

            for (Stock s : watchlist.getStocks()) {
                total++;
                try {
                    mappingRepo.createMapping(s.getId(), master.getId());
                    created++;
                } catch (Exception ex) {
                    // duplicates (unique_dem_day_event_master) and other per-row errors are skipped
                    log.debug("Skip mapping stock {} -> master {}: {}", s.getId(), master.getId(), ex.getMessage());
                }
            }
        }

        if (!missingWatchlists.isEmpty()) {
            log.warn("createMappingsForPreparedKeys({}): missing watchlist code(s): {}",
            date, String.join(",", missingWatchlists));
        }

        log.info("createMappingsForPreparedKeys({}): created {} of {} mappings for watchlist-codes [{}]",
                date, created, total, preparation.watchlistCodeSummary());
        return new Result(lastMasterCode, preparation.watchlistCodeSummary(), created, total, true);
    }

    /**
     * Cleanup: when a date has more than one day_event_master and one of them is the auto-generated
    * every-day master (code ends with "_" + watchlist-code), remove that redundant master
     * along with its day_event_map and day_event_entry rows. The genuine (manually created) master is kept.
     *
     * @return number of redundant masters removed
     */
    @Transactional
    public int cleanupRedundantMasters() {
        List<String> watchlistCodes = getWatchlistCodes();
        if (watchlistCodes.isEmpty()) {
            return 0;
        }

        java.util.Map<LocalDate, java.util.List<DayStockMovementKey>> byDate = new java.util.HashMap<>();
        for (DayStockMovementKey m : masterRepo.findAll()) {
            if (m.getDate() == null) continue;
            byDate.computeIfAbsent(m.getDate(), k -> new java.util.ArrayList<>()).add(m);
        }

        int removed = 0;
        for (String watchlistCode : watchlistCodes) {
            String suffix = "_" + watchlistCode; // auto-generated masters look like 12_JUN_26_MOVING_STOCK

            for (var entry : byDate.entrySet()) {
                java.util.List<DayStockMovementKey> masters = entry.getValue();
                if (masters.size() < 2) continue;

                boolean hasGenuine = masters.stream().anyMatch(m -> !isAutoMaster(m, suffix));
                if (!hasGenuine) continue; // only auto key(s) for this date — keep them

                for (DayStockMovementKey m : masters) {
                    if (isAutoMaster(m, suffix)) {
                        int entries = mappingRepo.deleteEntriesByMasterId(m.getId());
                        int maps = mappingRepo.deleteMappingsByMasterId(m.getId());
                        mappingRepo.deleteMasterById(m.getId());
                        removed++;
                        log.info("Cleanup: removed redundant master id={} code={} date={} ({} entries, {} mappings)",
                                m.getId(), m.getCode(), entry.getKey(), entries, maps);
                    }
                }
            }
        }

        if (removed > 0) {
            log.info("cleanupRedundantMasters: removed {} redundant master(s) for watchlist-codes [{}]",
                    removed, String.join(",", watchlistCodes));
        }
        return removed;
    }

    private boolean isAutoMaster(DayStockMovementKey m, String suffix) {
        return m.getCode() != null && m.getCode().endsWith(suffix);
    }

    private DayStockMovementKey getOrCreateDayStockMovementKey(String code, String watchlistCode, LocalDate date) {
        return masterRepo.findByCode(code).orElseGet(() -> {
            DayStockMovementKey saved = masterRepo.save(
                new DayStockMovementKey(code, "Every-day-event > " + watchlistCode, date));
            log.info("Created DayStockMovementKey with code={} id={}", saved.getCode(), saved.getId());
            return saved;
        });
    }

    private LocalDate resolveBackfillStartDate(LocalDate endDate) {
        var lastUpdatedCfg = SystemConfigEnum.DayStockMovementKeyMapEntry.LAST_UPDATED;
        String raw = systemConfigService
            .findByPurposeAndCode(lastUpdatedCfg.purpose(), lastUpdatedCfg.code())
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .orElse("");

        LocalDate lastExecutedDate = parseLastUpdatedDate(raw);
        if (lastExecutedDate == null) {
            return endDate;
        }
        return lastExecutedDate.plusDays(1);
    }

    private LocalDate parseLastUpdatedDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return Instant.parse(value).atZone(LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(value).withZoneSameInstant(LISBON).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
        } catch (Exception ignored) {
            return null;
        }
    }

    public record PreparedKey(String watchlistCode, DayStockMovementKey master) {}

    public record KeyPreparationResult(List<PreparedKey> preparedKeys,
                                       String watchlistCodeSummary,
                                       List<String> missingWatchlists,
                                       boolean marketClosed) {}

    public record Result(String masterCode, String watchlistCode, int mappingsCreated, int stocksTotal,
                         boolean watchlistFound) {}
}
