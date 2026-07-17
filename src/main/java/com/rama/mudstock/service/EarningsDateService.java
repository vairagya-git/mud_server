package com.rama.mudstock.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rama.mudstock.model.earnings.EarningsDate;
import com.rama.mudstock.model.stockwatchlist.Stock;
import com.rama.mudstock.repository.earnings.EarningsDateEntryRepository;
import com.rama.mudstock.repository.earnings.EarningsDateRepository;
import com.rama.mudstock.repository.stockwatchlist.StockRepository;

@Service
public class EarningsDateService {
    private final EarningsDateRepository repo;
    private final StockRepository stockRepository;
    private final EarningsDateEntryRepository entryRepository;

    public EarningsDateService(EarningsDateRepository repo, StockRepository stockRepository, EarningsDateEntryRepository entryRepository) {
        this.repo = repo;
        this.stockRepository = stockRepository;
        this.entryRepository = entryRepository;
    }

    public List<EarningsDate> listAll() { return repo.findAll(); }

    public List<java.util.Map<String, Object>> listAllEntries() { return entryRepository.listEntriesForFrontend(); }

    public List<java.util.Map<String, Object>> listUpcoming() { return repo.listUpcoming(); }

    public Optional<EarningsDate> get(Long id) { return repo.findById(id); }

    public EarningsDate save(EarningsDate e) {
        // ensure state default NEW for new entries
        boolean isNew = e.getId() == null;
        if (isNew && e.getStatus() == null) e.setStatus(EarningsDate.Status.NEW);
        EarningsDate saved = repo.save(e);
        // create earnings_date_entry rows for each period when a new earnings_date is created
        if (isNew) {
            if (entryRepository != null) entryRepository.createEntriesForEarningsDate(saved.getId(), saved.getStockId());
        }
        return saved;
    }

    public void delete(Long id) { repo.deleteById(id); }

    public List<Stock> allStocks() { return stockRepository.findAll(); }

    public Stock findOrCreateStockByTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) return null;
        java.util.Optional<Stock> sopt = stockRepository.findByTicker(ticker.trim().toUpperCase());
        if (sopt.isPresent()) return sopt.get();
        // Do not create missing stocks automatically because DB schema (CUSIP/CIK/CL etc.)
        // is managed externally and may require additional non-null fields.
        return null;
    }
}