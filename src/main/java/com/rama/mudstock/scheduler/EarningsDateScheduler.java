package com.rama.mudstock.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rama.mudstock.model.EarningsDate;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.repository.EarningsDateRepository;
import com.rama.mudstock.repository.StockRepository;
import com.rama.mudstock.service.DatePeriodFetcher;
import com.rama.mudstock.service.MassiveRestStockService;

@Component
public class EarningsDateScheduler {
    private final EarningsDateRepository earningsRepo;
    private final StockRepository stockRepo;
    private final MassiveRestStockService massiveService;
    private final DatePeriodFetcher datePeriodFetcher;
    private final Logger log = LoggerFactory.getLogger(EarningsDateScheduler.class);

    public EarningsDateScheduler(EarningsDateRepository earningsRepo, StockRepository stockRepo, MassiveRestStockService massiveService, DatePeriodFetcher datePeriodFetcher) {
        this.earningsRepo = earningsRepo;
        this.stockRepo = stockRepo;
        this.massiveService = massiveService;
        this.datePeriodFetcher = datePeriodFetcher;
    }

    

    // cron is configured in application.yml: earnings.cron
    @Scheduled(cron = "${earnings.cron}")
    public void pollEarnings() {
        log.info("EarningsDateScheduler: polling for NEW and PROCESSING entries");
        List<EarningsDate> list = earningsRepo.findAll();
        for (EarningsDate ed : list) {
            if (ed.getState() == EarningsDate.State.NEW || ed.getState() == EarningsDate.State.PROCESSING) {
                Long stockId = ed.getStockId();
                try {
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
                } catch (Exception e) {
                    log.error("Error processing earnings entry id={}: {}", ed.getId(), e.getMessage());
                }
            }
        }
    }
}
