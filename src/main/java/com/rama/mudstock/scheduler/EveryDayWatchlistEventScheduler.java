package com.rama.mudstock.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.model.DayEventMaster;
import com.rama.mudstock.model.SpecialWatchlistEnum;
import com.rama.mudstock.model.Watchlist;
import com.rama.mudstock.repository.DayEventMappingRepository;
import com.rama.mudstock.repository.DayEventMasterRepository;
import com.rama.mudstock.repository.WatchlistRepository;

@Component
//@Profile("every-day")
public class EveryDayWatchlistEventScheduler {
    private final WatchlistRepository watchlistRepo;
    private final DayEventMasterRepository masterRepo;
    private final DayEventMappingRepository mappingRepo;
    private final Logger log = LoggerFactory.getLogger(EveryDayWatchlistEventScheduler.class);

    @Value("${schedulers.everyDayEvent.cron:}")
    private String everyDayEventCron;

    @Value("${schedulers.everyDayEvent.watchlist-name:}")
    private String everyDayEventWatchlistName;

    public EveryDayWatchlistEventScheduler(WatchlistRepository watchlistRepo, DayEventMasterRepository masterRepo, DayEventMappingRepository mappingRepo) {
        this.watchlistRepo = watchlistRepo;
        this.masterRepo = masterRepo;
        this.mappingRepo = mappingRepo;
    }

    // Cron configured in application.yml: schedulers.everyDayEvent.cron
    @Scheduled(cron = "${schedulers.everyDayEvent.cron}")
    public void runEveryDayEvent() {
        log.info("EveryDayWatchlistEventScheduler: running scheduled job to populate day event for watchlist {}", SpecialWatchlistEnum.EVERY_DAY_EVENT);
        String watchlistName = everyDayEventWatchlistName;
        var maybe = watchlistRepo.findByName(watchlistName);
        if (maybe.isEmpty()) {
            log.warn("Watchlist '{}' not found; skipping job", watchlistName);
            return;
        }
        Watchlist w = maybe.get();

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmtDay = DateTimeFormatter.ofPattern("dd_MMM_yy", Locale.ENGLISH);
        String dayPart = today.format(fmtDay).toUpperCase(); // e.g., 01_JUN_26
        String code = String.format("%s_%s", dayPart, watchlistName);

        // avoid duplicate master with same code
        DayEventMaster master = masterRepo.findByCode(code).orElseGet(() -> {
            DayEventMaster m = new DayEventMaster(code, "Auto-generated every-day-event", today);
            DayEventMaster saved = masterRepo.save(m);
            log.info("Created DayEventMaster with code={} id={}", saved.getCode(), saved.getId());
            return saved;
        });

        int created = 0;
        for (com.rama.mudstock.model.Stock s : w.getStocks()) {
            try {
                mappingRepo.createMapping(s.getId(), master.getId());
                created++;
            } catch (Exception ex) {
                log.debug("Failed to create mapping for stock {} and master {}: {}", s.getId(), master.getId(), ex.getMessage());
            }
        }

        log.info("EveryDayWatchlistEventScheduler: created {} mappings for watchlist '{}' and dayEventMaster id={}", created, watchlistName, master.getId());
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onApplicationReady() {
        if (everyDayEventCron == null || everyDayEventCron.isBlank()) {
            log.warn("EveryDayWatchlistEventScheduler: schedulers.everyDayEvent.cron is not set or empty. @Scheduled may not be active.");
        } else {
            log.info("EveryDayWatchlistEventScheduler initialized with cron='{}'", everyDayEventCron);
        }
    }
}
