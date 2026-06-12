package com.rama.mudstock.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rama.mudstock.model.DayEventMaster;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.model.Watchlist;
import com.rama.mudstock.repository.DayEventMappingRepository;
import com.rama.mudstock.repository.DayEventMasterRepository;
import com.rama.mudstock.repository.WatchlistRepository;

/**
 * Shared logic for the "every day event" feature: for a given date, create a single
 * day_event_master entry for the configured watchlist (everyday-watchlist-code) and
 * map every stock in that watchlist into day_event_map.
 *
 * Used by both the scheduled job (for today) and the manual date form.
 */
@Service
public class EveryDayEventService {
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd_MMM_yy", Locale.ENGLISH);
    private final Logger log = LoggerFactory.getLogger(EveryDayEventService.class);

    private final WatchlistRepository watchlistRepo;
    private final DayEventMasterRepository masterRepo;
    private final DayEventMappingRepository mappingRepo;

    @Value("${everyday-watchlist-code:}")
    private String everydayWatchlistCode;

    public EveryDayEventService(WatchlistRepository watchlistRepo, DayEventMasterRepository masterRepo,
                                DayEventMappingRepository mappingRepo) {
        this.watchlistRepo = watchlistRepo;
        this.masterRepo = masterRepo;
        this.mappingRepo = mappingRepo;
    }

    public String getWatchlistCode() {
        return everydayWatchlistCode == null ? "" : everydayWatchlistCode.trim();
    }

    /**
     * Create (or reuse) the day_event_master for the given date and map all stocks in the
     * configured watchlist. Runs in a transaction so the lazy watchlist.stocks collection can
     * be read even when called outside a web request (e.g. from the scheduler thread).
     */
    @Transactional
    public Result populateForDate(LocalDate date) {
        String watchlistCode = getWatchlistCode();
        if (watchlistCode.isEmpty()) {
            log.warn("everyday-watchlist-code is not configured; skipping populate");
            return new Result(null, watchlistCode, 0, 0, false);
        }

        var maybe = watchlistRepo.findByCode(watchlistCode);
        if (maybe.isEmpty()) {
            log.warn("Watchlist with code '{}' not found; skipping populate", watchlistCode);
            return new Result(null, watchlistCode, 0, 0, false);
        }
        Watchlist w = maybe.get();

        String dayPart = date.format(DAY_FMT).toUpperCase(); // e.g. 01_JUN_26
        String code = String.format("%s_%s", dayPart, watchlistCode);

        DayEventMaster master = masterRepo.findByCode(code).orElseGet(() -> {
            DayEventMaster saved = masterRepo.save(new DayEventMaster(code, "Every-day-event > " + watchlistCode, date));
            log.info("Created DayEventMaster with code={} id={}", saved.getCode(), saved.getId());
            return saved;
        });

        int created = 0;
        int total = 0;
        for (Stock s : w.getStocks()) {
            total++;
            try {
                mappingRepo.createMapping(s.getId(), master.getId());
                created++;
            } catch (Exception ex) {
                // duplicates (unique_dem_day_event_master) and other per-row errors are skipped
                log.debug("Skip mapping stock {} -> master {}: {}", s.getId(), master.getId(), ex.getMessage());
            }
        }

        log.info("populateForDate({}): created {} of {} mappings for watchlist '{}' dayEventMaster id={}",
                date, created, total, watchlistCode, master.getId());
        return new Result(master.getCode(), watchlistCode, created, total, true);
    }

    /**
     * Cleanup: when a date has more than one day_event_master and one of them is the auto-generated
     * every-day master (code ends with "_" + everyday-watchlist-code), remove that redundant master
     * along with its day_event_map and day_event_entry rows. The genuine (manually created) master is kept.
     *
     * @return number of redundant masters removed
     */
    @Transactional
    public int cleanupRedundantMasters() {
        String watchlistCode = getWatchlistCode();
        if (watchlistCode.isEmpty()) {
            return 0;
        }
        String suffix = "_" + watchlistCode; // auto-generated masters look like 12_JUN_26_MOVING_STOCK

        java.util.Map<LocalDate, java.util.List<DayEventMaster>> byDate = new java.util.HashMap<>();
        for (DayEventMaster m : masterRepo.findAll()) {
            if (m.getEventDate() == null) continue;
            byDate.computeIfAbsent(m.getEventDate(), k -> new java.util.ArrayList<>()).add(m);
        }

        int removed = 0;
        for (var entry : byDate.entrySet()) {
            java.util.List<DayEventMaster> masters = entry.getValue();
            if (masters.size() < 2) continue;

            boolean hasGenuine = masters.stream().anyMatch(m -> !isAutoMaster(m, suffix));
            if (!hasGenuine) continue; // only auto master(s) for this date — keep them

            for (DayEventMaster m : masters) {
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

        if (removed > 0) {
            log.info("cleanupRedundantMasters: removed {} redundant '{}' master(s)", removed, watchlistCode);
        }
        return removed;
    }

    private boolean isAutoMaster(DayEventMaster m, String suffix) {
        return m.getCode() != null && m.getCode().endsWith(suffix);
    }

    public record Result(String masterCode, String watchlistCode, int mappingsCreated, int stocksTotal,
                         boolean watchlistFound) {}
}
