package com.rama.mudstock.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rama.mudstock.model.EarningsDate;
import com.rama.mudstock.model.Stock;
import com.rama.mudstock.repository.EarningsDateRepository;
import com.rama.mudstock.repository.StockRepository;

@Service
public class EarningsDateService {
    private final EarningsDateRepository repo;
    private final StockRepository stockRepository;

    public EarningsDateService(EarningsDateRepository repo, StockRepository stockRepository) {
        this.repo = repo;
        this.stockRepository = stockRepository;
    }

    public List<EarningsDate> listAll() { return repo.findAll(); }

    public Optional<EarningsDate> get(Long id) { return repo.findById(id); }

    public EarningsDate save(EarningsDate e) {
        // ensure state default NEW for new entries
        if (e.getId() == null && e.getState() == null) e.setState(EarningsDate.State.NEW);
        return repo.save(e);
    }

    public void delete(Long id) { repo.deleteById(id); }

    public List<Stock> allStocks() { return stockRepository.findAll(); }
}
