package com.rama.mudstock.scheduler.earnings;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.model.earnings.EarningsDate;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.earnings.EarningsDateEntryRepository;
import com.rama.mudstock.repository.earnings.EarningsDateRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;
import com.rama.mudstock.service.DatePeriodFetcher;
import com.rama.mudstock.service.MassiveRestStockService;

@Component
@Profile("cronjob")
@ConditionalOnProperty(prefix = "earnings", name = "enabled", havingValue = "true")
public class EarningsDateScheduler {
    private final EarningsDateRepository earningsRepo;
    private final StockRepository stockRepo;
    private final MassiveRestStockService massiveService;
    private final DatePeriodFetcher datePeriodFetcher;
    private final EarningsDateEntryRepository entryRepository;
    private final Logger log = LoggerFactory.getLogger(EarningsDateScheduler.class);

    public EarningsDateScheduler(EarningsDateRepository earningsRepo, StockRepository stockRepo, MassiveRestStockService massiveService, DatePeriodFetcher datePeriodFetcher, EarningsDateEntryRepository entryRepository) {
        this.earningsRepo = earningsRepo;
        this.stockRepo = stockRepo;
        this.massiveService = massiveService;
        this.datePeriodFetcher = datePeriodFetcher;
        this.entryRepository = entryRepository;
    }

    

    // cron is configured in application.yml: earnings.cron
    @Scheduled(cron = "${earnings.cron}")
    public void pollEarnings() {
        log.info("polling for NEW and PROCESSING entries");
        List<EarningsDate> list = earningsRepo.findAll();
        for (EarningsDate ed : list) {
            if (ed.getStatus() == EarningsDate.Status.NEW || ed.getStatus() == EarningsDate.Status.PROCESSING) {
                Long stockId = ed.getStockId();
                try {
                    // mark as PROCESSING if we are starting from NEW
                    if (ed.getStatus() == EarningsDate.Status.NEW) {
                        ed.setStatus(EarningsDate.Status.PROCESSING);
                        earningsRepo.save(ed);
                    }
                    Stock stock = stockRepo.findById(stockId).orElse(null);
                    if (stock == null) {
                        log.warn("No stock found for id={}", stockId);
                        continue;
                    }
                    String ticker = stock.getTicker();
                    if (ticker == null || ticker.isBlank()) {
                        log.warn("Stock {} has empty ticker", stockId);
                        continue;
                    }
                    try {
                        datePeriodFetcher.fetchAllForTickerAndEarningsDate(ticker, ed.getEarningsDate(), ed.getId(), ed.getStockId());
                    } catch (Exception ex) {
                        log.error("Failed to fetch period data for {} (earningsId={}): {}", ticker, ed.getId(), ex.getMessage());
                    }

                    // after attempting fetches, if all entries are done mark earnings entry as PROCESSED
                    try {
                        boolean allDone = entryRepository.allEntriesDoneForEarningsDate(ed.getId());
                        if (allDone) {
                            ed.setStatus(EarningsDate.Status.PROCESSED);
                            earningsRepo.save(ed);
                        }
                    } catch (Exception ex3) {
                        log.error("Error finalizing earningsId={}: {}", ed.getId(), ex3.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Error processing earnings entry id={}: {}", ed.getId(), e.getMessage());
                }
            }
        }
    }
}
